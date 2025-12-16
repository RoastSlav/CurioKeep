package org.rostislav.curiokeep.modules.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ConstraintsXml(
        @JacksonXmlProperty(isAttribute = true) BigDecimal min,
        @JacksonXmlProperty(isAttribute = true) BigDecimal max,
        @JacksonXmlProperty(isAttribute = true) Integer minLength,
        @JacksonXmlProperty(isAttribute = true) Integer maxLength,
        @JacksonXmlProperty(isAttribute = true) String pattern,
        @JacksonXmlProperty(isAttribute = true) Boolean multi,
        @JacksonXmlProperty(isAttribute = true) Boolean uniqueWithinCollection
) {
}
