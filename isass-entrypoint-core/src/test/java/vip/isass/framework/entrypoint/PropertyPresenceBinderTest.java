// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PropertyPresenceBinderTest {

    @Test
    void bindsNestedObjectsAndCollectionsWithoutTreatingMissingFieldsAsPresent() {
        Payload child = new Payload();
        Payload root = new Payload();
        root.setChildren(List.of(child));

        PropertyPresenceBinder.bind(root, Map.of(
                "name", nullValue(),
                "children", List.of(Map.of("description", "value"))));

        assertTrue(root.isPropertyPresent("name"));
        assertTrue(root.isPropertyPresent("children"));
        assertFalse(root.isPropertyPresent("description"));
        assertTrue(child.isPropertyPresent("description"));
        assertFalse(child.isPropertyPresent("name"));

        @SuppressWarnings("unchecked")
        Map<String, Object> projected = (Map<String, Object>) PropertyPresenceBinder.project(root, Map.of(
                "name", nullValue(),
                "description", "must-be-removed",
                "children", List.of(Map.of("name", "must-be-removed", "description", "value"))));
        assertTrue(projected.containsKey("name"));
        assertFalse(projected.containsKey("description"));
        @SuppressWarnings("unchecked")
        Map<String, Object> projectedChild = (Map<String, Object>) ((List<?>) projected.get("children")).getFirst();
        assertTrue(projectedChild.containsKey("description"));
        assertFalse(projectedChild.containsKey("name"));
    }

    @Test
    void traversesRecordComponentsWhenBindingAndProjectingNestedEntities() {
        Payload payload = new Payload();
        Envelope envelope = new Envelope(List.of(new Item(payload)));

        PropertyPresenceBinder.bind(envelope, Map.of(
                "items", List.of(Map.of("entity", Map.of("name", "value")))));

        assertTrue(payload.isPropertyPresent("name"));
        assertFalse(payload.isPropertyPresent("description"));

        @SuppressWarnings("unchecked")
        Map<String, Object> projected = (Map<String, Object>) PropertyPresenceBinder.project(envelope, Map.of(
                "items", List.of(Map.of("entity", Map.of(
                        "name", "value",
                        "description", "must-be-removed")))));
        @SuppressWarnings("unchecked")
        Map<String, Object> item = (Map<String, Object>) ((List<?>) projected.get("items")).getFirst();
        @SuppressWarnings("unchecked")
        Map<String, Object> entity = (Map<String, Object>) item.get("entity");
        assertTrue(entity.containsKey("name"));
        assertFalse(entity.containsKey("description"));
    }

    @Test
    void matchesMapEntriesByTheirSerializedKey() {
        Payload payload = new Payload();
        Map<Long, Payload> values = new TreeMap<>();
        values.put(123L, payload);

        PropertyPresenceBinder.bind(values, Map.of(
                "123", Map.of("name", "value")));

        assertTrue(payload.isPropertyPresent("name"));
        assertFalse(payload.isPropertyPresent("description"));

        @SuppressWarnings("unchecked")
        Map<String, Object> projected = (Map<String, Object>) PropertyPresenceBinder.project(values, Map.of(
                "123", Map.of("name", "value", "description", "must-be-removed")));
        @SuppressWarnings("unchecked")
        Map<String, Object> projectedPayload = (Map<String, Object>) projected.get("123");
        assertTrue(projectedPayload.containsKey("name"));
        assertFalse(projectedPayload.containsKey("description"));
    }

    private Object nullValue() {
        return new Object();
    }

    static final class Payload implements PropertyPresenceAware {
        private String name;
        private String description;
        private List<Payload> children;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<Payload> getChildren() { return children; }
        public void setChildren(List<Payload> children) { this.children = children; }
    }

    private record Envelope(List<Item> items) {
    }

    private record Item(Payload entity) {
    }
}
