package org.rostislav.curiokeep.modules.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowStepXml(
        @JacksonXmlProperty(isAttribute = true) String type,
        @JacksonXmlProperty(isAttribute = true) String field,
        @JacksonXmlProperty(isAttribute = true) String fields,
        @JacksonXmlProperty(isAttribute = true) String providers,
        @JacksonXmlProperty(isAttribute = true) String label
) {
}
