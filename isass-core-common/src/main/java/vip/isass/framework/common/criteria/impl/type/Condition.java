// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.criteria.impl.type;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonSerialize;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * ORM 无关的条件操作符；Java API 按语义分组，JSON 仍使用原有枚举名。
 */
@JsonSerialize(using = Condition.Serializer.class)
@JsonDeserialize(using = Condition.Deserializer.class)
public sealed interface Condition permits Condition.Logical, Condition.Compare, Condition.Membership,
        Condition.NullCheck, Condition.Text, Condition.Array, Condition.Json, Condition.Existence {

    String name();

    enum Logical implements Condition {AND, OR, NOT}

    enum Compare implements Condition {
        EQUAL, NOT_EQUAL, GREATER_THAN, GREATER_THAN_EQUAL, LESS_THAN, LESS_THAN_EQUAL
    }

    enum Membership implements Condition {IN, NOT_IN}

    enum NullCheck implements Condition {IS_NULL, IS_NOT_NULL}

    enum Text implements Condition {IS_EMPTY, IS_NOT_EMPTY, START_WITH, LIKE, NOT_LIKE}

    enum Array implements Condition {CONTAINS_ALL, CONTAINS_ANY}

    enum Json implements Condition {
        JSON_OBJECT_PATH_EQUAL, JSON_OBJECT_PATH_LIKE, JSON_ARRAY_CONTAINS, JSON_ARRAY_CONTAINS_ANY, JSON_ARRAY_CONTAINS_ALL
    }

    enum Existence implements Condition {EXISTS, NOT_EXISTS}

    static Condition fromName(String name) {
        Condition condition = Lookup.BY_NAME.get(name);
        if (condition == null) {
            throw new IllegalArgumentException("未知条件操作符: " + name);
        }
        return condition;
    }

    final class Lookup {
        private static final Map<String, Condition> BY_NAME = Stream.of(Logical.values(), Compare.values(), Membership.values(), NullCheck.values(), Text.values(), Array.values(), Json.values(), Existence.values()).flatMap(Arrays::stream).collect(Collectors.toUnmodifiableMap(Condition::name, Function.identity()));

        private Lookup() {
        }
    }

    final class Serializer extends ValueSerializer<Condition> {
        @Override
        public void serialize(Condition value, JsonGenerator generator, SerializationContext context) {
            generator.writeString(value.name());
        }
    }

    final class Deserializer extends ValueDeserializer<Condition> {
        @Override
        public Condition deserialize(JsonParser parser, DeserializationContext context) {
            return fromName(context.readTree(parser).asString());
        }
    }
}
