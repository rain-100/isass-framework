// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.association;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses table-level NoCode relationship markers before template rendering. */
public final class TableAssociationParser {

    private static final Pattern ASSOCIATION = Pattern.compile(
            "\\[关联表-(列表|单体)-([A-Za-z][A-Za-z0-9]*)(.*?)]", Pattern.DOTALL);
    private static final Pattern TREE_CASCADE = Pattern.compile(
            "\\[树结构-\\s*cascadeDelete\\s*=\\s*true\\s*]", Pattern.CASE_INSENSITIVE);

    private TableAssociationParser() {
    }

    public static List<GeneratorAssociation> parse(String sourceEntity, String comment) {
        List<GeneratorAssociation> result = new ArrayList<>();
        Matcher matcher = ASSOCIATION.matcher(comment == null ? "" : comment);
        while (matcher.find()) {
            GeneratorAssociation.Kind kind = "列表".equals(matcher.group(1))
                    ? GeneratorAssociation.Kind.MANY : GeneratorAssociation.Kind.ONE;
            String target = matcher.group(2);
            Map<String, String> options = options(matcher.group(3));
            String property = options.getOrDefault("property", kind == GeneratorAssociation.Kind.ONE
                    ? lowerCamel(target) : plural(lowerCamel(target)));
            String localKey = options.get("localKey");
            String targetKey = options.get("targetKey");
            if (localKey == null || targetKey == null) {
                if (kind == GeneratorAssociation.Kind.ONE) {
                    localKey = lowerCamel(target) + "Id";
                    targetKey = "id";
                } else {
                    localKey = "id";
                    targetKey = lowerCamel(sourceEntity) + "Id";
                }
            }
            result.add(new GeneratorAssociation(property, target, kind, localKey, targetKey,
                    Boolean.parseBoolean(options.getOrDefault("cascadeDelete", "false"))));
        }
        return List.copyOf(result);
    }

    public static boolean treeCascadeDelete(String comment) {
        return TREE_CASCADE.matcher(comment == null ? "" : comment).find();
    }

    public static String description(String comment) {
        String value = comment == null ? "" : comment;
        value = ASSOCIATION.matcher(value).replaceAll("");
        value = TREE_CASCADE.matcher(value).replaceAll("");
        return value.trim();
    }

    private static Map<String, String> options(String text) {
        Map<String, String> result = new LinkedHashMap<>();
        for (String token : text.split(";")) {
            int separator = token.indexOf('=');
            if (separator > 0) {
                result.put(token.substring(0, separator).trim(), token.substring(separator + 1).trim());
            }
        }
        return result;
    }

    private static String lowerCamel(String value) {
        return value.substring(0, 1).toLowerCase(Locale.ROOT) + value.substring(1);
    }

    private static String plural(String value) {
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.endsWith("y") && value.length() > 1
                && "aeiou".indexOf(lower.charAt(lower.length() - 2)) < 0) {
            return value.substring(0, value.length() - 1) + "ies";
        }
        if (lower.endsWith("s") || lower.endsWith("x") || lower.endsWith("z")
                || lower.endsWith("ch") || lower.endsWith("sh")) {
            return value + "es";
        }
        return value + "s";
    }
}
