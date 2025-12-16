package org.rostislav.curiokeep.modules.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProviderXml(
        @JacksonXmlProperty(isAttribute = true) String key,
        @JacksonXmlProperty(isAttribute = true) Boolean enabled,

        @JacksonXmlProperty(localName = "supports")
        SupportsXml supports,

        Integer priority
) {
}
