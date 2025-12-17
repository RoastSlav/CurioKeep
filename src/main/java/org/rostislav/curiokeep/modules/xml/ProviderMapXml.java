package org.rostislav.curiokeep.modules.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProviderMapXml(
        @JacksonXmlProperty(isAttribute = true) String provider,
        @JacksonXmlProperty(isAttribute = true) String path,
        @JacksonXmlProperty(isAttribute = true) String transform
) {
}
