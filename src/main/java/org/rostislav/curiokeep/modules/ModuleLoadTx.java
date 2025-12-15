package org.rostislav.curiokeep.modules;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.rostislav.curiokeep.modules.contract.*;
import org.rostislav.curiokeep.modules.xml.ModuleXml;
import org.springframework.core.io.Resource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static org.rostislav.curiokeep.modules.ModuleUtil.*;
import static org.rostislav.curiokeep.modules.ModuleUtil.jsonb;

@Service
public class ModuleLoadTx {
    private final ModuleXsdValidator xsdValidator;
    private final ModuleXmlParser xmlParser;
    private final ModuleCompiler moduleCompiler;
    private final ModuleContractValidator contractValidator;
    private final NamedParameterJdbcTemplate jdbc;
    private final ObjectMapper objectMapper;

    public ModuleLoadTx(ModuleXsdValidator xsdValidator,
                        ModuleXmlParser xmlParser,
                        ModuleCompiler moduleCompiler,
                        ModuleContractValidator contractValidator,
                        NamedParameterJdbcTemplate jdbc) {
        this.xsdValidator = xsdValidator;
        this.xmlParser = xmlParser;
        this.moduleCompiler = moduleCompiler;
        this.contractValidator = contractValidator;
        this.jdbc = jdbc;

        objectMapper = JsonMapper.builder()
                .defaultPropertyInclusion(JsonInclude.Value.construct(NON_NULL, NON_NULL))
                .findAndAddModules()
                .build();
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loadOneModule(Resource r, String sourceName) throws Exception {
        String xml = readUtf8(r);

        xsdValidator.validate(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        ModuleXml moduleXml = xmlParser.parse(xml);

        ModuleContract contract = moduleCompiler.compile(moduleXml);

        JsonNode contractJson = objectMapper.valueToTree(contract);
        contractValidator.validate(contractJson);

        validateSemantics(contract, sourceName);

        upsertBuiltinModule(contract, xml, contractJson);
    }

    private void validateSemantics(ModuleContract m, String sourceName) {
        String moduleKey = m.key();

        // OWNED mandatory
        if (m.states().stream().noneMatch(s -> "OWNED".equals(s.key()))) {
            throw new IllegalStateException("[" + sourceName + "] Module '" + moduleKey + "' must define state OWNED");
        }

        // unique state keys
        ensureUnique(
                m.states().stream().map(StateContract::key).toList(),
                "[" + sourceName + "] Module '" + moduleKey + "' has duplicate state keys"
        );

        // unique field keys
        ensureUnique(
                m.fields().stream().map(FieldContract::key).toList(),
                "[" + sourceName + "] Module '" + moduleKey + "' has duplicate field keys"
        );

        // provider mappings must reference declared providers
        java.util.Set<String> providerKeys = m.providers().stream().map(ProviderContract::key).collect(Collectors.toSet());
        for (FieldContract f : m.fields()) {
            for (ProviderMapping pm : f.providerMappings()) {
                if (!providerKeys.contains(pm.provider())) {
                    throw new IllegalStateException("[" + sourceName + "] Module '" + moduleKey + "': field '" +
                            f.key() + "' references unknown provider '" + pm.provider() + "'");
                }
                if (pm.path() == null || !pm.path().startsWith("/")) {
                    throw new IllegalStateException("[" + sourceName + "] Module '" + moduleKey + "': field '" +
                            f.key() + "' has invalid provider mapping path '" + pm.path() + "' (must start with /)");
                }
            }
        }

        // workflows must reference existing fields/providers (basic)
        java.util.Set<String> fieldKeys = m.fields().stream().map(FieldContract::key).collect(Collectors.toSet());
        for (WorkflowContract wf : m.workflows()) {
            for (WorkflowStep step : wf.steps()) {
                if (step.field() != null && !fieldKeys.contains(step.field())) {
                    throw new IllegalStateException("[" + sourceName + "] Module '" + moduleKey + "': workflow '" +
                            wf.key() + "' references unknown field '" + step.field() + "'");
                }
                if (step.fields() != null) {
                    for (String fk : step.fields()) {
                        if (!fieldKeys.contains(fk)) {
                            throw new IllegalStateException("[" + sourceName + "] Module '" + moduleKey + "': workflow '" +
                                    wf.key() + "' references unknown field '" + fk + "'");
                        }
                    }
                }
                if (step.providers() != null) {
                    for (String pk : step.providers()) {
                        if (!providerKeys.contains(pk)) {
                            throw new IllegalStateException("[" + sourceName + "] Module '" + moduleKey + "': workflow '" +
                                    wf.key() + "' references unknown provider '" + pk + "'");
                        }
                    }
                }
            }
        }
    }

    private void upsertBuiltinModule(ModuleContract module, String xmlRaw, JsonNode contractJson) {
        String checksum = sha256Hex(xmlRaw);

        UUID existingId = jdbc.query(
                "SELECT id FROM module_definition WHERE module_key = :k AND checksum = :c",
                new MapSqlParameterSource().addValue("k", module.key()).addValue("c", checksum),
                rs -> rs.next() ? rs.getObject(1, UUID.class) : null
        );

        if (existingId != null) {
            // No change, skip deletes/inserts entirely
            return;
        }

        UUID moduleId;
        try {
            moduleId = upsertModuleDefinition(module, xmlRaw, checksum, contractJson);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize module contract JSON for module " + module.key(), e);
        }

        replaceModuleStates(moduleId, module);
        replaceModuleFields(moduleId, module);
    }


    private UUID upsertModuleDefinition(ModuleContract module, String xmlRaw, String checksum, JsonNode contractJson) throws JsonProcessingException {
        String sql = """
                INSERT INTO module_definition (
                    module_key, name, version, source, checksum, xml_raw, definition_json, created_at, updated_at
                ) VALUES (
                    :module_key, :name, :version, 'BUILTIN', :checksum, :xml_raw, :definition_json, now(), now()
                )
                ON CONFLICT (module_key) DO UPDATE SET
                    name = EXCLUDED.name,
                    version = EXCLUDED.version,
                    source = 'BUILTIN',
                    checksum = EXCLUDED.checksum,
                    xml_raw = EXCLUDED.xml_raw,
                    definition_json = EXCLUDED.definition_json,
                    updated_at = now()
                RETURNING id
                """;

        MapSqlParameterSource params = new MapSqlParameterSource()
                .addValue("module_key", module.key())
                .addValue("name", module.name())
                .addValue("version", module.version())
                .addValue("checksum", checksum)
                .addValue("xml_raw", xmlRaw)
                .addValue("definition_json", jsonb(objectMapper.writeValueAsString(contractJson)));

        UUID id = jdbc.queryForObject(sql, params, UUID.class);
        if (id == null)
            throw new IllegalStateException("Upsert module_definition returned null id for " + module.key());
        return id;
    }

    private void replaceModuleStates(UUID moduleId, ModuleContract module) {
        jdbc.update("DELETE FROM module_state WHERE module_id = :mid",
                new MapSqlParameterSource("mid", moduleId));

        var states = Optional.ofNullable(module.states()).orElse(List.of());
        if (states.isEmpty()) return;

        String insert = """
                INSERT INTO module_state (module_id, state_key, label, sort_order)
                VALUES (:mid, :state_key, :label, :sort_order)
                """;

        MapSqlParameterSource[] batch = states.stream()
                .map(s -> new MapSqlParameterSource()
                        .addValue("mid", moduleId)
                        .addValue("state_key", s.key())
                        .addValue("label", s.label())
                        .addValue("sort_order", s.order()))
                .toArray(MapSqlParameterSource[]::new);

        jdbc.batchUpdate(insert, batch);
    }

    private void replaceModuleFields(UUID moduleId, ModuleContract module) {
        jdbc.update("DELETE FROM module_field WHERE module_id = :mid",
                new MapSqlParameterSource("mid", moduleId));

        var fields = Optional.ofNullable(module.fields()).orElse(List.of());
        if (fields.isEmpty()) return;

        String insert = """
                INSERT INTO module_field (
                    id, module_id, field_key, label, field_type,
                    required, searchable, filterable, sortable,
                    default_value, enum_values, provider_mappings,
                    sort_order
                ) VALUES (
                    gen_random_uuid(), :mid, :field_key, :label, :field_type,
                    :required, :searchable, :filterable, :sortable,
                    :default_value, :enum_values, :provider_mappings,
                    :sort_order
                )
                """;

        MapSqlParameterSource[] batch = fields.stream()
                .map(f -> {
                    try {
                        return new MapSqlParameterSource()
                                .addValue("mid", moduleId)
                                .addValue("field_key", f.key())
                                .addValue("label", f.label())
                                .addValue("field_type", f.type().name())
                                .addValue("required", f.required())
                                .addValue("searchable", f.searchable())
                                .addValue("filterable", f.filterable())
                                .addValue("sortable", f.sortable())
                                .addValue("default_value", jsonb(objectMapper.writeValueAsString(f.defaultValue())))
                                .addValue("enum_values", jsonb(objectMapper.writeValueAsString(f.enumValues())))
                                .addValue("provider_mappings", jsonb(objectMapper.writeValueAsString(f.providerMappings())))
                                .addValue("sort_order", f.order());
                    } catch (JsonProcessingException e) {
                        throw new IllegalStateException("Failed to serialize field JSON for module " + module.key() +
                                ", field " + f.key(), e);
                    }
                })
                .toArray(MapSqlParameterSource[]::new);

        jdbc.batchUpdate(insert, batch);
    }
}
