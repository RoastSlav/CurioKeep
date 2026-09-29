package org.rostislav.curiokeep.modules.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MigrationMappingXml(
        @JacksonXmlProperty(isAttribute = true) String from,
        @JacksonXmlProperty(isAttribute = true) String to
) {
}
