// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.initialization;

import vip.isass.framework.nocode.initialization.NocodeInitializationDataService.ImportResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Service-scoped execution contract behind the system initialization import/export endpoints.
 * Implementations execute the selected service's entities without routing them again.
 */
public interface INocodeInitializationService {

    ImportResult importData(String serviceName, Map<String, ? extends Collection<?>> document);

    Map<String, List<?>> exportData(String serviceName, Collection<String> entityNames);

}
