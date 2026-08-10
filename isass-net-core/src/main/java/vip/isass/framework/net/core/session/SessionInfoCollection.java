// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.net.core.session;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;
import vip.isass.framework.common.map.MultiKeyMultiValueBiMap;
import vip.isass.framework.common.map.MultiValueBiMap;

import java.util.Collection;

@Getter
@Setter
@ToString
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class SessionInfoCollection {

    private Collection<DisplaySession> sessions;

    private MultiValueBiMap<String, String> userAndSessionMap;

    private MultiValueBiMap<String, String> aliasAndSessionMap;

    private MultiKeyMultiValueBiMap<String, String> sessionAndTagMap;

}
