package org.rostislav.curiokeep.modules.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UiHintsXml(
        @JacksonXmlProperty(isAttribute = true) String widget,
        @JacksonXmlProperty(isAttribute = true) String placeholder,
        @JacksonXmlProperty(isAttribute = true) String helpText,
        @JacksonXmlProperty(isAttribute = true) String group,
        @JacksonXmlProperty(isAttribute = true) Boolean hidden
) {
}
