// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.generator.association;

/** Structured, directional association metadata consumed by templates. */
public record GeneratorAssociation(
        String property,
        String targetEntity,
        Kind kind,
        String localKey,
        String targetKey,
        boolean cascadeDelete
) {
    public enum Kind { ONE, MANY }
}
