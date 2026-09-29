package org.rostislav.curiokeep.modules;

/** A module needs a newer application than the one running, so it is refused rather than half working. */
public class ModuleRequiresNewerAppException extends IllegalStateException {

    public ModuleRequiresNewerAppException(String message) {
        super(message);
    }
}
