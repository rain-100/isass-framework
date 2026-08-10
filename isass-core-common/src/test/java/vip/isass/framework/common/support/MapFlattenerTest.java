// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support;

import java.util.HashMap;
import java.util.Map;

/**
 * MapFlattener 测试类
 *
 * @author Rain
 */
public class MapFlattenerTest {

    public static void main(String[] args) {
        Map<String, Object> originalMap = new HashMap<>();
        originalMap.put("name", "Alice");
        originalMap.put("age", 30);

        Map<String, Object> addressMap = new HashMap<>();
        addressMap.put("street", "123 Main St");
        addressMap.put("city", "New York");

        originalMap.put("address", addressMap);

        Map<String, Object> flattenedMap = MapFlattener.flattenMap(originalMap);

        // 输出结果
        for (Map.Entry<String, Object> entry : flattenedMap.entrySet()) {
            System.out.println("Key: " + entry.getKey() + ", Value: " + entry.getValue());
        }
    }
}