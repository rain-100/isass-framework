// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.web.security.authorization;

/** Request for the roles required to access a resource URI. */
public record UriRoleCodesRequest(String uri) {
}
