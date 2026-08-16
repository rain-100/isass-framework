// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.stream;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileStreamTest {

    @Test
    void openInputStreamAdaptsWriterWithoutLoadingAllContent() throws Exception {
        FileStream stream = new FileStream("test.txt", "text/plain", 4L, true,
                output -> output.write("data".getBytes(StandardCharsets.UTF_8)));

        try (InputStream input = stream.openInputStream()) {
            assertEquals("data", new String(input.readAllBytes(), StandardCharsets.UTF_8));
        }
        assertThrows(IllegalStateException.class, () -> stream.writeTo(new ByteArrayOutputStream()));
    }

    @Test
    void inputStreamExposesSourceFailure() throws Exception {
        FileStream stream = new FileStream("test.txt", "text/plain", null, true,
                output -> {
                    output.write('a');
                    throw new IOException("storage failed");
                });

        try (InputStream input = stream.openInputStream()) {
            assertEquals('a', input.read());
            IOException exception = assertThrows(IOException.class, input::read);
            assertEquals("storage failed", exception.getCause().getMessage());
        }
    }

    @Test
    void writeToCanOnlyBeUsedOnce() throws Exception {
        FileStream stream = new FileStream("test.txt", "text/plain", 4L, true,
                output -> output.write("data".getBytes(StandardCharsets.UTF_8)));
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        stream.writeTo(output);

        assertEquals("data", output.toString(StandardCharsets.UTF_8));
        assertThrows(IllegalStateException.class, stream::openInputStream);
    }
}
