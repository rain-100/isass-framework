// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.web;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RespTest {

    @Test
    void detailMessageShouldBeSerializableWhenPresent() {
        Resp<String> resp = Resp.<String>bizFail()
            .setDetailMessage("NullPointerException: path");

        assertEquals("NullPointerException: path", resp.getDetailMessage());
        assertTrue(resp.toString().contains("\"detailMessage\":\"NullPointerException: path\""));
    }
}
