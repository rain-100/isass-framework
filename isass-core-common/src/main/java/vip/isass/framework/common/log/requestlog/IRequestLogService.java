// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.log.requestlog;

import vip.isass.framework.common.support.api.ApiService;

import java.util.List;

public interface IRequestLogService extends ApiService {

    void add(RequestLog requestLog);

    void addBatch(List<RequestLog> requestLogs);

}
