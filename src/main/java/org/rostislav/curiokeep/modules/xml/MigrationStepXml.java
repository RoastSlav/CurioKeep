package org.rostislav.curiokeep.modules.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MigrationStepXml(
        @JacksonXmlProperty(isAttribute = true) String op,
        @JacksonXmlProperty(isAttribute = true) String from,
        @JacksonXmlProperty(isAttribute = true) String to,
        @JacksonXmlProperty(isAttribute = true) String field,
        @JacksonXmlProperty(isAttribute = true) String value,
        @JacksonXmlProperty(isAttribute = true) String transform,
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "mapping")
        List<MigrationMappingXml> mappings
) {
}
