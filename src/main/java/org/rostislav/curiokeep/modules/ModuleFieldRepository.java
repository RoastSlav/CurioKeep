package org.rostislav.curiokeep.modules;

import org.rostislav.curiokeep.modules.entities.ModuleFieldEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ModuleFieldRepository extends JpaRepository<ModuleFieldEntity, UUID> {
    List<ModuleFieldEntity> findAllByModuleId(UUID moduleId);
}
