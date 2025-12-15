package org.rostislav.curiokeep.modules;

import org.rostislav.curiokeep.modules.entities.ModuleStateEntity;
import org.rostislav.curiokeep.modules.entities.ModuleStateId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ModuleStateRepository extends JpaRepository<ModuleStateEntity, ModuleStateId> {
    List<ModuleStateEntity> findAllByModuleId(UUID moduleId);
    Optional<ModuleStateEntity> findByModuleIdAndStateKey(UUID moduleId, String stateKey);
}
