// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.association;

import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Parses DDL ownership and human descriptions; relations are declared in Java. */
public final class TableAssociationParser {

    private static final Pattern DOMAIN = Pattern.compile("\\[--domain:([^]]*)]");
    private static final Pattern PACKAGE_SEGMENT = Pattern.compile("[a-z][a-z0-9]*");
    private static final String SUBDOMAIN_PREFIX = "--subdomain:";
    private static final Set<String> RESERVED_SUBDOMAIN_NAMES = Set.of(
            "application", "domain", "infrastructure", "interfaces");
    private static final Pattern ASSOCIATION = Pattern.compile(
            "\\[关联表-(列表|单体)-([A-Za-z][A-Za-z0-9]*)(.*?)]", Pattern.DOTALL);
    private static final Pattern TREE_CASCADE = Pattern.compile(
            "\\[树结构-\\s*cascadeDelete\\s*=\\s*true\\s*]", Pattern.CASE_INSENSITIVE);

    private TableAssociationParser() {
    }

    /**
     * Returns the domain and optional subdomain declared by one table metadata marker.
     *
     * <p>Supported forms are {@code [--domain:domain]} and
     * {@code [--domain:domain;--subdomain:subdomain]}.</p>
     */
    public static DomainMetadata domainMetadata(String comment) {
        Matcher matcher = DOMAIN.matcher(comment == null ? "" : comment);
        if (!matcher.find()) return null;
        String declarationText = matcher.group(1);
        if (matcher.find()) {
            throw new IllegalArgumentException("表注释只能声明一个 domain 元数据标记");
        }

        String[] declarations = declarationText.split(";", -1);
        String domain = declarations.length == 0 ? "" : declarations[0].trim();
        validatePackageSegment("domain", domain);

        String subdomain = null;
        for (int index = 1; index < declarations.length; index++) {
            String declaration = declarations[index].trim();
            if (!declaration.startsWith(SUBDOMAIN_PREFIX)) {
                throw new IllegalArgumentException("未知的 domain 元数据参数: " + declaration);
            }
            if (subdomain != null) {
                throw new IllegalArgumentException("表注释只能声明一个 subdomain");
            }
            subdomain = declaration.substring(SUBDOMAIN_PREFIX.length()).trim();
            validatePackageSegment("subdomain", subdomain);
            if (RESERVED_SUBDOMAIN_NAMES.contains(subdomain)) {
                throw new IllegalArgumentException("subdomain 不能使用 DDD 技术层名称: " + subdomain);
            }
        }
        return new DomainMetadata(domain, subdomain);
    }

    /** Returns the package-safe domain declared by the table metadata marker. */
    public static String domain(String comment) {
        DomainMetadata metadata = domainMetadata(comment);
        return metadata == null ? null : metadata.domain();
    }

    /** Returns the optional package-safe subdomain declared by the table metadata marker. */
    public static String subdomain(String comment) {
        DomainMetadata metadata = domainMetadata(comment);
        return metadata == null ? null : metadata.subdomain();
    }

    public static String description(String comment) {
        String value = comment == null ? "" : comment;
        value = DOMAIN.matcher(value).replaceAll("");
        value = ASSOCIATION.matcher(value).replaceAll("");
        value = TREE_CASCADE.matcher(value).replaceAll("");
        return value.trim();
    }

    private static void validatePackageSegment(String name, String value) {
        if (!PACKAGE_SEGMENT.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " 必须是小写 Java 包名段: " + value);
        }
    }

    /** Parsed domain ownership metadata for one generated table. */
    public record DomainMetadata(String domain, String subdomain) {
    }
}
