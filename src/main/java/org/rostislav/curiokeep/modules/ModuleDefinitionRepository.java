package org.rostislav.curiokeep.modules;

import org.rostislav.curiokeep.modules.entities.ModuleDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ModuleDefinitionRepository extends JpaRepository<ModuleDefinitionEntity, UUID> {
    Optional<ModuleDefinitionEntity> findByModuleKey(String moduleKey);

    boolean existsByModuleKey(String moduleKey);
}
