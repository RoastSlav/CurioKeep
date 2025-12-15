package org.rostislav.curiokeep.modules.xml;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record MetaXml(
        @JacksonXmlProperty(localName = "authors")
        AuthorsXml authors,

        String license,
        String homepage,
        String repository,
        String icon,

        @JacksonXmlProperty(localName = "tags")
        TagsXml tags,

        String minAppVersion
) {
        public List<AuthorXml> authorsList() {
                return authors == null || authors.author() == null ? List.of() : authors.author();
        }

        public List<String> tagsList() {
                return tags == null || tags.tag() == null ? List.of() : tags.tag();
        }
}
