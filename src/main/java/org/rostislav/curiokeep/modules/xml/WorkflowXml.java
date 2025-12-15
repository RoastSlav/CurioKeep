package org.rostislav.curiokeep.modules.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WorkflowXml(
        @JacksonXmlProperty(isAttribute = true) String key,
        @JacksonXmlProperty(isAttribute = true) String label,

        @JacksonXmlElementWrapper(useWrapping = false)
        @JacksonXmlProperty(localName = "step")
        List<WorkflowStepXml> steps
) {
}
