package org.rostislav.curiokeep.modules.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FieldXml(
        @JacksonXmlProperty(isAttribute = true) String key,
        @JacksonXmlProperty(isAttribute = true) String label,
        @JacksonXmlProperty(isAttribute = true) String type,

        @JacksonXmlProperty(isAttribute = true) Boolean required,
        @JacksonXmlProperty(isAttribute = true) Boolean searchable,
        @JacksonXmlProperty(isAttribute = true) Boolean filterable,
        @JacksonXmlProperty(isAttribute = true) Boolean sortable,
        @JacksonXmlProperty(isAttribute = true) Integer order,

        // wrappers (these names MUST match XML wrapper elements)
        @JacksonXmlProperty(localName = "identifiers") IdentifiersXml identifiers,
        @JacksonXmlProperty(localName = "enumValues") EnumValuesXml enumValues,
        ConstraintsXml constraints,
        UiHintsXml ui,
        @JacksonXmlProperty(localName = "providerMappings") ProviderMappingsXml providerMappings,

        String defaultValue
) {
}
