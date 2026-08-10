// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.nocode.http;

import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.service.annotation.HttpExchange;
import tools.jackson.databind.JsonNode;

import java.net.URI;

/** One dynamic Spring HTTP service client shared by all nocode contracts. */
@HttpExchange(accept = MediaType.APPLICATION_JSON_VALUE)
public interface NocodeHttpExchange {

    @HttpExchange
    JsonNode exchange(
            HttpMethod method,
            URI uri,
            @RequestParam MultiValueMap<String, String> query,
            @RequestBody(required = false) Object body
    );
}
