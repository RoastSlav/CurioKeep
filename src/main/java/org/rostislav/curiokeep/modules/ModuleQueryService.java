package org.rostislav.curiokeep.modules;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.rostislav.curiokeep.modules.api.dto.ModuleDetailsResponse;
import org.rostislav.curiokeep.modules.api.dto.ModuleRawXmlResponse;
import org.rostislav.curiokeep.modules.api.dto.ModuleSummaryResponse;
import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.rostislav.curiokeep.modules.contract.ModuleSource;
import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;

@Service
public class ModuleQueryService {

    private static final Logger log = LoggerFactory.getLogger(ModuleQueryService.class);
    private final ObjectMapper objectMapper;
    private final ModuleDefinitionRepository modules;

    public ModuleQueryService(ModuleDefinitionRepository modules) {
        this.modules = modules;

        objectMapper = JsonMapper.builder()
                .defaultPropertyInclusion(JsonInclude.Value.construct(NON_NULL, NON_NULL))
                .findAndAddModules()
                .build();
    }

    @Transactional(readOnly = true)
    public List<ModuleSummaryResponse> listAll() {
        return modules.findAll(Sort.by(Sort.Direction.ASC, "moduleKey"))
                .stream()
                .map(ModuleSummaryResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ModuleDetailsResponse getByKey(String moduleKey) {
        ModuleDefinitionEntity e = modules.findByModuleKey(moduleKey)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MODULE_NOT_FOUND"));

        ModuleContract contract = objectMapper.convertValue(e.getDefinitionJson(), ModuleContract.class);

        ModuleSource source = ModuleSource.valueOf(e.getSource().name());

        return new ModuleDetailsResponse(
                e.getId(),
                e.getModuleKey(),
                e.getName(),
                e.getVersion(),
                source,
                e.getChecksum(),
                contract,
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    public Optional<ModuleDefinitionEntity> getEntityById(UUID id) {
        return modules.findById(id);
    }

    public ModuleContract getContract(ModuleDefinitionEntity e) {
        return objectMapper.convertValue(e.getDefinitionJson(), ModuleContract.class);
    }

    @Transactional(readOnly = true)
    public ModuleRawXmlResponse getRawXml(String moduleKeyRaw) {
        String key = normalizeKey(moduleKeyRaw);

        ModuleDefinitionEntity m = modules.findByModuleKey(key)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "MODULE_NOT_FOUND"));

        return new ModuleRawXmlResponse(m.getXmlRaw());
    }

    private String normalizeKey(String raw) {
        if (raw == null) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "moduleKey is required");
        String k = raw.trim().toLowerCase();
        if (k.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "moduleKey is required");
        return k;
    }
}
