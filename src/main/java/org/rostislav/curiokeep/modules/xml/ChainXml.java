package org.rostislav.curiokeep.modules.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ChainXml(
        @JacksonXmlProperty(isAttribute = true) String from,
        @JacksonXmlProperty(isAttribute = true) String to,
        @JacksonXmlProperty(isAttribute = true) String idType
) {
}
