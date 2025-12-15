package org.rostislav.curiokeep.modules;

import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.rostislav.curiokeep.modules.xml.ModuleXml;
import org.springframework.stereotype.Component;

@Component
public class ModuleXmlParser {
    private final XmlMapper xmlMapper = new XmlMapper();

    public ModuleXml parse(String xml) throws Exception {
        return xmlMapper.readValue(xml, ModuleXml.class);
    }
}
