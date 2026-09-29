package org.rostislav.curiokeep.modules;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Component;

/** The version of this application, which a module's {@code minAppVersion} is checked against. */
@Component
public class AppVersion {

    private final String version;

    /** The version is unknown when the application does not run from a build, for example from an IDE; then no module is refused. */
    @Autowired
    public AppVersion(ObjectProvider<BuildProperties> build) {
        BuildProperties properties = build.getIfAvailable();
        this.version = properties == null ? null : properties.getVersion();
    }

    private AppVersion(String version) {
        this.version = version;
    }

    static AppVersion of(String version) {
        return new AppVersion(version);
    }

    /**
     * True when this application is at least {@code minimum}. A suffix on the application's own version such as
     * {@code -SNAPSHOT} is ignored: a snapshot of 1.2.0 already has what 1.2.0 needs.
     */
    public boolean satisfies(String minimum) {
        if (minimum == null || !ModuleVersion.isValid(version)) return true;
        String numbers = version.trim().split("-", 2)[0];
        return ModuleVersion.compare(numbers, minimum) >= 0;
    }

    public String value() {
        return version;
    }
}
