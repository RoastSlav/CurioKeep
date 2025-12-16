package org.rostislav.curiokeep.modules.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AuthorXml(
        @JacksonXmlProperty(isAttribute = true) String name,
        @JacksonXmlProperty(isAttribute = true) String email,
        @JacksonXmlProperty(isAttribute = true) String url
) {
}
