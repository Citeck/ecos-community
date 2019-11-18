package ru.citeck.ecos.records;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.*;
import org.alfresco.util.ParameterCheck;

import java.util.Iterator;
import java.util.function.BiConsumer;
import java.util.function.Function;

public class RecordMeta {

    private RecordRef id = RecordRef.EMPTY;

    private ObjectNode attributes = JsonNodeFactory.instance.objectNode();

    public RecordMeta() {
    }

    public RecordMeta(RecordMeta other, Function<RecordRef, RecordRef> idMapper) {
        setId(idMapper.apply(other.getId()));
        setAttributes(other.getAttributes());
    }

    public RecordMeta(String id) {
        this.id = new RecordRef(id);
    }

    public RecordMeta(RecordRef id) {
        this.id = id;
    }

    public RecordMeta(RecordRef id, ObjectNode attributes) {
        this.id = id;
        setAttributes(attributes);
    }

    public RecordRef getId() {
        return id;
    }

    public void setId(RecordRef id) {
        this.id = id != null ? id : RecordRef.EMPTY;
    }

    public void forEach(BiConsumer<String, JsonNode> consumer) {
        Iterator<String> names = attributes.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            consumer.accept(name, attributes.get(name));
        }
    }

    public ObjectNode getAttributes() {
        return attributes;
    }

    public JsonNode getAttribute(String name) {
        return attributes.path(name);
    }

    public boolean has(String name) {
        return hasAttribute(name);
    }

    public boolean hasAttribute(String name) {
        JsonNode att = attributes.path(name);
        return !isEmpty(att);
    }

    public <T> T get(String name, T orElse) {
        return getAttribute(name, orElse);
    }

    public <T> T getAttribute(String name, T orElse) {

        ParameterCheck.mandatory("name", name);
        ParameterCheck.mandatory("orElse", orElse);

        JsonNode att = attributes.get(name);
        if (isEmpty(att)) {
            return orElse;
        }

        Object value;

        if (orElse instanceof String) {
            value = att.asText();
        } else if (orElse instanceof Integer) {
            value = att.asInt((Integer) orElse);
        } else if (orElse instanceof Long) {
            value = att.asLong((Long) orElse);
        } else if (orElse instanceof Double) {
            value = att.asDouble((Double) orElse);
        } else if (orElse instanceof Float) {
            value = (float) att.asDouble((Float) orElse);
        } else if (orElse instanceof Boolean) {
            value = att.asBoolean((Boolean) orElse);
        } else if (orElse instanceof JsonNode) {
            value = att;
        } else {
            value = orElse;
        }

        return (T) value;
    }

    public void set(String name, String value) {
        setAttribute(name, value);
    }

    public void setAttribute(String name, String value) {
        attributes.put(name, value);
    }

    private boolean isEmpty(JsonNode value) {
        return value == null || value.isMissingNode() || value.isNull();
    }

    public void setAttributes(ObjectNode attributes) {
        if (attributes != null) {
            this.attributes = attributes.deepCopy();
        } else {
            this.attributes = JsonNodeFactory.instance.objectNode();
        }
    }

    @Override
    public String toString() {
        return "{" +
                "\"id\":" + id +
                ", \"attributes\":" + attributes +
                '}';
    }
}