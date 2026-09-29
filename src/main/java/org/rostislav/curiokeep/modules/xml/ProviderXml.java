package org.rostislav.curiokeep.modules.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ProviderXml(
        @JacksonXmlProperty(isAttribute = true) String key,
        @JacksonXmlProperty(isAttribute = true) Boolean enabled,

        @JacksonXmlProperty(localName = "supports")
        SupportsXml supports,

        Integer priority,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "chain")
        List<ChainXml> chains
) {
}
