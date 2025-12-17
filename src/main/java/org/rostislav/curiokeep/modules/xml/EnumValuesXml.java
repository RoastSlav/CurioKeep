package org.rostislav.curiokeep.modules.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EnumValuesXml(
        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "value")
        List<EnumValueXml> value
) {
}
