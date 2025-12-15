package org.rostislav.curiokeep.modules;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "curiokeep.modules")
public record ModuleProps(
        String classpath,
        String schema,
        String externalDir
) {
}
