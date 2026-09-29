package org.rostislav.curiokeep.items;

import org.rostislav.curiokeep.modules.contract.ModuleContract;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

/** Which item states a module allows and how a requested state key is normalised. */
final class ItemStateRules {

    private ItemStateRules() {
    }

    /** A blank key means the module's first declared state (or OWNED when it declares none); anything else is upper-cased. */
    static String normalize(String stateKey, ModuleContract contract) {
        if (stateKey == null || stateKey.isBlank()) {
            return contract.states().isEmpty() ? "OWNED" : contract.states().getFirst().key();
        }
        return stateKey.trim().toUpperCase(Locale.ROOT);
    }

    /** A blank key is accepted (it normalises to the default); a key the module does not declare is refused. */
    static void validate(ModuleContract contract, String stateKey) {
        if (stateKey == null || stateKey.isBlank()) return;
        String key = stateKey.trim().toUpperCase(Locale.ROOT);
        if (contract.states().stream().noneMatch(s -> s.key().equalsIgnoreCase(key))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "INVALID_STATE");
        }
    }
}
