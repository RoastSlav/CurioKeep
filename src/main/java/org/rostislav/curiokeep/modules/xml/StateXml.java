package org.rostislav.curiokeep.modules.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StateXml(
        @JacksonXmlProperty(isAttribute = true) String key,
        @JacksonXmlProperty(isAttribute = true) String label,
        @JacksonXmlProperty(isAttribute = true) Integer order
) {
}
