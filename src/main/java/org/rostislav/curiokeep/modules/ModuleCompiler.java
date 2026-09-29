package org.rostislav.curiokeep.modules;

import org.rostislav.curiokeep.modules.contract.*;
import org.rostislav.curiokeep.modules.xml.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ModuleCompiler {

        private static final Logger log = LoggerFactory.getLogger(ModuleCompiler.class);

        private static boolean bool(Boolean b, boolean def) {
                return b == null ? def : b;
        }

        private static int safeInt(Integer v, int def) {
                return v == null ? def : v;
        }

        private static List<String> splitCsv(String csv) {
                if (csv == null || csv.isBlank()) return List.of();
                return Arrays.stream(csv.split(","))
                                .map(String::trim)
                                .filter(s -> !s.isBlank())
                                .collect(Collectors.toList());
        }

    public ModuleContract compile(ModuleXml xml) {
        Objects.requireNonNull(xml, "module xml is null");

        var meta = compileMeta(xml.meta());
        var states = compileStates(xml.states().state());
        var providers = compileProviders(xml.providers().provider());
        var fields = compileFields(xml.fields().field());
        var workflows = compileWorkflows(xml.workflows().workflow());
        var migrations = compileMigrations(xml.migrationsList());

        // sort for stable output (useful for checksums + diffing)
        states = states.stream()
                .sorted(Comparator.comparingInt(StateContract::order).thenComparing(StateContract::key))
                .toList();

        providers = providers.stream()
                .sorted(Comparator.comparingInt(ProviderContract::priority).thenComparing(ProviderContract::key))
                .toList();

        fields = fields.stream()
                .sorted(Comparator.comparingInt(FieldContract::order).thenComparing(FieldContract::key))
                .toList();

        return new ModuleContract(
                xml.key(),
                xml.version(),
                xml.name(),
                xml.description(),
                meta,
                states,
                providers,
                fields,
                workflows,
                Map.of(), // extensions (not supported by XSD yet)
                migrations
        );
    }

    private ModuleMeta compileMeta(MetaXml metaXml) {
        if (metaXml == null) return null;

        List<Author> authors = Optional.ofNullable(metaXml.authors())
                .orElse(new AuthorsXml(List.of())).author()
                .stream()
                .map(this::compileAuthor)
                .toList();

        List<String> tags = Optional.ofNullable(metaXml.tags())
                .orElse(new TagsXml(List.of())).tag();

        return new ModuleMeta(
                authors,
                metaXml.license(),
                metaXml.homepage(),
                metaXml.repository(),
                metaXml.icon(),
                tags,
                metaXml.minAppVersion()
        );
    }

    private Author compileAuthor(AuthorXml a) {
        return new Author(a.name(), a.email(), a.url());
    }

    private List<StateContract> compileStates(List<StateXml> stateXmls) {
        return Optional.ofNullable(stateXmls).orElse(List.of()).stream()
                .map(s -> new StateContract(
                        s.key(),
                        s.label(),
                        safeInt(s.order(), 0),
                        bool(s.active(), true),
                        bool(s.deprecated(), false),
                        Map.of()
                ))
                .toList();
    }

    private List<ProviderContract> compileProviders(List<ProviderXml> providerXmls) {
        return Optional.ofNullable(providerXmls).orElse(List.of()).stream()
                .map(p -> {
                    boolean enabled = (p.enabled() == null) || p.enabled();
                    int priority = safeInt(p.priority(), 100);

                    List<IdentifierType> supports = Optional.ofNullable(p.supports())
                            .orElse(new SupportsXml(new ArrayList<>())).identifiers()
                            .stream()
                            .map(IdentifierRefXml::type)
                            .map(this::toIdentifierType)
                            .toList();

                    return new ProviderContract(
                            p.key(),
                            enabled,
                            priority,
                            supports,
                            Map.of(),
                            Optional.ofNullable(p.chains()).orElse(List.of()).stream()
                                    .map(c -> new ProviderChain(c.from(), c.to(), c.idType() == null ? IdentifierType.CUSTOM : toIdentifierType(c.idType())))
                                    .toList()
                    );
                })
                .toList();
    }

    // ----------------------------
    // Conversions + helpers
    // ----------------------------

    private Constraints compileConstraints(ConstraintsXml c) {
        if (c == null) return null;
        String pattern = c.pattern() == null || c.pattern().isBlank() ? null : c.pattern();
        return new Constraints(
                c.min() == null ? null : c.min().doubleValue(),
                c.max() == null ? null : c.max().doubleValue(),
                c.minLength(), c.maxLength(), pattern, c.multi(), c.uniqueWithinCollection());
    }

    /** The XML holds the default as text; a NUMBER or BOOLEAN field gets it as that type. One that cannot be read as such is dropped with a warning, so an old module keeps loading. */
    private Object compileDefault(FieldType type, String text) {
        if (text == null || text.isBlank()) return null;
        String trimmed = text.trim();
        try {
            switch (type) {
                case NUMBER:
                    return new BigDecimal(trimmed);
                case BOOLEAN:
                    if ("true".equalsIgnoreCase(trimmed) || "false".equalsIgnoreCase(trimmed)) return Boolean.valueOf(trimmed.toLowerCase(Locale.ROOT));
                    break;
                default:
                    return text;
            }
        } catch (NumberFormatException e) {
            // falls through to the warning below
        }
        log.warn("Ignoring default value '{}': it is not a valid {}", trimmed, type);
        return null;
    }

    private List<FieldContract> compileFields(List<FieldXml> fieldXmls) {
        return Optional.ofNullable(fieldXmls).orElse(List.of()).stream()
                .map(f -> {
                    List<IdentifierType> identifiers = Optional.ofNullable(f.identifiers())
                            .orElse(new IdentifiersXml(new ArrayList<>())).identifier()
                            .stream()
                            .map(IdentifierRefXml::type)
                            .map(this::toIdentifierType)
                            .toList();

                    List<EnumValue> enumValues = Optional.ofNullable(f.enumValues())
                            .orElse(new EnumValuesXml(new ArrayList<>())).value()
                            .stream()
                            .map(ev -> new EnumValue(ev.key(), ev.label()))
                            .toList();

                    List<ProviderMapping> mappings = Optional.ofNullable(f.providerMappings())
                            .orElse(new ProviderMappingsXml(new ArrayList<>())).map()
                            .stream()
                            .map(pm -> new ProviderMapping(
                                    pm.provider(),
                                    pm.path(),
                                    pm.transform()
                            ))
                            .toList();

                    var uiHints = compileUi(f.ui());
                    warnIfCustomIdentifierMissingHelpText(f.key(), identifiers, uiHints);


                    return new FieldContract(
                            f.key(),
                            f.label(),
                            toFieldType(f.type()),
                            bool(f.required(), false),
                            bool(f.searchable(), false),
                            bool(f.filterable(), false),
                            bool(f.sortable(), false),
                            safeInt(f.order(), 0),
                            bool(f.active(), true),
                            bool(f.deprecated(), false),
                            compileDefault(toFieldType(f.type()), f.defaultValue()),
                            identifiers,
                            enumValues,
                            compileConstraints(f.constraints()),
                            uiHints,
                            mappings,
                            Map.of(),
                            f.replacedBy() == null || f.replacedBy().isBlank() ? null : f.replacedBy().trim()
                    );
                })
                .toList();
    }

    private UiHints compileUi(UiXml uiXml) {
        if (uiXml == null) return null;

        return new UiHints(
                uiXml.widget(),
                uiXml.placeholder(),
                uiXml.helpText(),
                uiXml.group(),
                uiXml.hidden()
        );
    }

    private void warnIfCustomIdentifierMissingHelpText(String fieldKey, List<IdentifierType> identifiers, UiHints uiHints) {
        if (!identifiers.contains(IdentifierType.CUSTOM)) {
            return;
        }

        String helpText = uiHints == null ? null : uiHints.helpText();
        if (helpText == null || helpText.isBlank()) {
            log.warn("Field '{}' declares CUSTOM identifier but ui.helpText is missing", fieldKey);
        }
    }

    private List<MigrationContract> compileMigrations(List<MigrationXml> migrationXmls) {
        return migrationXmls.stream()
                .map(m -> new MigrationContract(m.to() == null ? null : m.to().trim(),
                        Optional.ofNullable(m.steps()).orElse(List.of()).stream().map(this::compileMigrationStep).toList()))
                .sorted(Comparator.comparing(MigrationContract::to, ModuleVersion::compare))
                .toList();
    }

    private MigrationStep compileMigrationStep(MigrationStepXml s) {
        return new MigrationStep(
                MigrationOp.valueOf(s.op().trim().toUpperCase(Locale.ROOT)),
                blankToNull(s.from()), blankToNull(s.to()), blankToNull(s.field()),
                s.value(), // may be an empty string on purpose, so it is not trimmed or nulled
                blankToNull(s.transform()),
                Optional.ofNullable(s.mappings()).orElse(List.of()).stream()
                        .map(mapping -> new MigrationStep.ValueMapping(mapping.from(), mapping.to())).toList());
    }

    private static String blankToNull(String text) {
        return text == null || text.isBlank() ? null : text.trim();
    }

    private List<WorkflowContract> compileWorkflows(List<WorkflowXml> workflowXmls) {
        return Optional.ofNullable(workflowXmls).orElse(List.of()).stream()
                .map(wf -> new WorkflowContract(
                        wf.key(),
                        wf.label(),
                        Optional.ofNullable(wf.steps()).orElse(List.of()).stream()
                                .map(this::compileStep)
                                .toList(),
                        Map.of()
                ))
                .toList();
    }

    private WorkflowStep compileStep(WorkflowStepXml s) {
        var type = toWorkflowStepType(s.type());

        // Your XSD uses CSV strings for fields/providers on a step. Keep it that way for v1.
        List<String> fields = splitCsv(s.fields());
        List<String> providers = splitCsv(s.providers());

        return new WorkflowStep(
                type,
                s.field(),
                fields,
                providers,
                s.query(),
                s.label(),
                Map.of()
        );
    }

    private FieldType toFieldType(String raw) {
        if (raw == null) throw new IllegalArgumentException("field type is null");
        return FieldType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }

    private IdentifierType toIdentifierType(String raw) {
        if (raw == null) throw new IllegalArgumentException("identifier type is null");
        return IdentifierType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }

    private WorkflowStepType toWorkflowStepType(String raw) {
        if (raw == null) throw new IllegalArgumentException("workflow step type is null");
        return WorkflowStepType.valueOf(raw.trim().toUpperCase(Locale.ROOT));
    }
}
