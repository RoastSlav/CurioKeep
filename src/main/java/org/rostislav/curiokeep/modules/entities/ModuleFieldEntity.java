package org.rostislav.curiokeep.modules.entities;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.UUID;

@Entity
@Table(
        name = "module_field",
        uniqueConstraints = @UniqueConstraint(name = "uk_module_field_module_key", columnNames = {"module_id", "field_key"})
)
public class ModuleFieldEntity {

    @Id
    @GeneratedValue
    @Column(name = "id", columnDefinition = "uuid")
    private UUID id;
    @Column(name = "module_id", nullable = false, columnDefinition = "uuid")
    private UUID moduleId;
    @Column(name = "field_key", nullable = false)
    private String fieldKey;
    @Column(name = "label", nullable = false)
    private String label;
    @Enumerated(EnumType.STRING)
    @Column(name = "field_type", nullable = false)
    private FieldType fieldType;
    @Column(name = "required", nullable = false)
    private boolean required = false;
    @Column(name = "searchable", nullable = false)
    private boolean searchable = false;
    @Column(name = "filterable", nullable = false)
    private boolean filterable = false;
    @Column(name = "sortable", nullable = false)
    private boolean sortable = false;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "default_value", columnDefinition = "jsonb")
    private JsonNode defaultValue;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "enum_values", columnDefinition = "jsonb")
    private JsonNode enumValues;
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "provider_mappings", columnDefinition = "jsonb")
    private JsonNode providerMappings;
    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 0;
    @Column(name = "active", nullable = false)
    private boolean active = true;
    @Column(name = "deprecated", nullable = false)
    private boolean deprecated = false;

    // getters/setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getModuleId() {
        return moduleId;
    }

    public void setModuleId(UUID moduleId) {
        this.moduleId = moduleId;
    }

    public String getFieldKey() {
        return fieldKey;
    }

    public void setFieldKey(String fieldKey) {
        this.fieldKey = fieldKey;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public FieldType getFieldType() {
        return fieldType;
    }

    public void setFieldType(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public boolean isSearchable() {
        return searchable;
    }

    public void setSearchable(boolean searchable) {
        this.searchable = searchable;
    }

    public boolean isFilterable() {
        return filterable;
    }

    public void setFilterable(boolean filterable) {
        this.filterable = filterable;
    }

    public boolean isSortable() {
        return sortable;
    }

    public void setSortable(boolean sortable) {
        this.sortable = sortable;
    }

    public JsonNode getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(JsonNode defaultValue) {
        this.defaultValue = defaultValue;
    }

    public JsonNode getEnumValues() {
        return enumValues;
    }

    public void setEnumValues(JsonNode enumValues) {
        this.enumValues = enumValues;
    }

    public JsonNode getProviderMappings() {
        return providerMappings;
    }

    public void setProviderMappings(JsonNode providerMappings) {
        this.providerMappings = providerMappings;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isDeprecated() {
        return deprecated;
    }

    public void setDeprecated(boolean deprecated) {
        this.deprecated = deprecated;
    }

    public enum FieldType {TEXT, NUMBER, DATE, BOOLEAN, ENUM, TAGS, LINK, JSON}
}