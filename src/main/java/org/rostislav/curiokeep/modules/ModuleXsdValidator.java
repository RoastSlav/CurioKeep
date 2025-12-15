package org.rostislav.curiokeep.modules;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.InputStream;

@Component
public class ModuleXsdValidator {

    private final javax.xml.validation.Schema schema;

    public ModuleXsdValidator(ModuleProps props, ResourceLoader loader) throws Exception {
        Resource xsd = loader.getResource(props.schema());
        this.schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                .newSchema(xsd.getURL());
    }

    public void validate(InputStream xml) throws Exception {
        var validator = schema.newValidator();
        validator.validate(new StreamSource(xml));
    }
}
