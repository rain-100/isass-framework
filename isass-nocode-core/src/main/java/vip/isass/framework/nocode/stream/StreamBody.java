package vip.isass.framework.nocode.stream;

import java.io.IOException;
import java.io.OutputStream;

/**
 * 向目标输出流写入文件内容的传输无关数据源。
 */
@FunctionalInterface
public interface StreamBody {

    void writeTo(OutputStream output) throws IOException;
}
