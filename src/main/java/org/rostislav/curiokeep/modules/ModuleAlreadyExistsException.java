package org.rostislav.curiokeep.modules;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class ModuleAlreadyExistsException extends ResponseStatusException {
    private final String moduleKey;

    public ModuleAlreadyExistsException(String moduleKey) {
        super(HttpStatus.CONFLICT, "Module already exists: " + moduleKey);
        this.moduleKey = moduleKey;
    }

    public String moduleKey() {
        return moduleKey;
    }
}
