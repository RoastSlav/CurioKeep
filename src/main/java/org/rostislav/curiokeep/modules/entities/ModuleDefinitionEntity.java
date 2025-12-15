package org.rostislav.curiokeep.modules.entities;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.rostislav.curiokeep.modules.contract.ModuleSource;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "module_definition")
public class ModuleDefinitionEntity {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;
    @Column(name = "module_key", nullable = false, unique = true)
    private String moduleKey;
    @Column(name = "name", nullable = false)
    private String name;
    @Column(name = "version", nullable = false)
    private String version;
    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private ModuleSource source;
    @Column(name = "checksum", nullable = false)
    private String checksum;
    @Column(name = "xml_raw", nullable = false, columnDefinition = "text")
    private String xmlRaw;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "definition_json", nullable = false, columnDefinition = "jsonb")
    private JsonNode definitionJson;
    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    void prePersist() {
        var now = OffsetDateTime.now();
        if (createdAt == null) createdAt = now;
        if (updatedAt == null) updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = OffsetDateTime.now();
    }

    // getters/setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getModuleKey() {
        return moduleKey;
    }

    public void setModuleKey(String moduleKey) {
        this.moduleKey = moduleKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public ModuleSource getSource() {
        return source;
    }

    public void setSource(ModuleSource source) {
        this.source = source;
    }

    public String getChecksum() {
        return checksum;
    }

    public void setChecksum(String checksum) {
        this.checksum = checksum;
    }

    public String getXmlRaw() {
        return xmlRaw;
    }

    public void setXmlRaw(String xmlRaw) {
        this.xmlRaw = xmlRaw;
    }

    public JsonNode getDefinitionJson() {
        return definitionJson;
    }

    public void setDefinitionJson(JsonNode definitionJson) {
        this.definitionJson = definitionJson;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}