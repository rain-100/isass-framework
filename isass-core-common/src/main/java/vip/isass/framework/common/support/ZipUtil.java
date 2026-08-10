// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.common.support;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.function.Consumer;
import java.util.zip.ZipInputStream;

public class ZipUtil {

    /**
     * zip 解压。按照 charsetNames 的顺序打开压缩包
     *
     * @param inputStream
     * @param consumer
     * @param charsetNames
     */
    public static void unZip(InputStream inputStream, Consumer<ZipInputStream> consumer, String... charsetNames) {
        unzip(inputStream, consumer, 0, charsetNames);
    }

    private static void unzip(InputStream inputStream,
                              Consumer<ZipInputStream> consumer,
                              int currentCharsetIndex,
                              String... charsetNames) {
        String charsetName = charsetNames[currentCharsetIndex];
        Charset charset = Charset.forName(charsetName);

        try (ZipInputStream zipInputStream = new ZipInputStream(inputStream, charset)) {
            consumer.accept(zipInputStream);
        } catch (IllegalArgumentException e) {
            if ("MALFORMED".equals(e.getMessage())) {
                if (currentCharsetIndex < charsetNames.length - 1) {
                    unzip(inputStream, consumer, currentCharsetIndex + 1, charsetNames);
                } else {
                    throw new RuntimeException("处理zip文件失败，所有指定的字符集均无法打开压缩包", e);
                }
            } else {
                throw new RuntimeException("处理zip文件失败: " + e.getMessage(), e);
            }
        } catch (Exception e) {
            throw new RuntimeException("处理zip文件失败: " + e.getMessage(), e);
        }
    }
}
