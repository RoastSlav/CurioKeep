package org.rostislav.curiokeep.modules.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import tools.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ModuleXml(
        @JacksonXmlProperty(isAttribute = true) String key,
        @JacksonXmlProperty(isAttribute = true) String version,

        String name,
        String description,
        MetaXml meta,

        @JacksonXmlProperty(localName = "states")
        StatesXml states,

        @JacksonXmlProperty(localName = "providers")
        ProvidersXml providers,

        @JacksonXmlProperty(localName = "fields")
        FieldsXml fields,

        @JacksonXmlProperty(localName = "workflows")
        WorkflowsXml workflows
) {
        public List<StateXml> statesList() {
                return states == null || states.state() == null ? List.of() : states.state();
        }

        public List<ProviderXml> providersList() {
                return providers == null || providers.provider() == null ? List.of() : providers.provider();
        }

        public List<FieldXml> fieldsList() {
                return fields == null || fields.field() == null ? List.of() : fields.field();
        }

        public List<WorkflowXml> workflowsList() {
                return workflows == null || workflows.workflow() == null ? List.of() : workflows.workflow();
        }
}