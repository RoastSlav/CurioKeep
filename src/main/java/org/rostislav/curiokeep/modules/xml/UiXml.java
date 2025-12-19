package org.rostislav.curiokeep.modules.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record UiXml(
        @JacksonXmlProperty(localName = "placeholder") String placeholder,
        @JacksonXmlProperty(localName = "helpText") String helpText,
        @JacksonXmlProperty(isAttribute = true) String widget,
        @JacksonXmlProperty(isAttribute = true) String group,
        @JacksonXmlProperty(isAttribute = true) Boolean hidden
) {
}
