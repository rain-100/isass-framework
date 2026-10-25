// SPDX-License-Identifier: LGPL-3.0-only

package vip.isass.framework.entrypoint.stream;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileStreamTest {

    @Test
    void unusedRemoteSourceCanBeClosedAndCannotBeConsumedLater() throws Exception {
        var closed = new AtomicInteger();
        var stream = new FileStream("image.png", "image/png", null, false,
                output -> output.write(1), () -> closed.incrementAndGet());
        stream.close();
        stream.close();
        assertEquals(1, closed.get());
        assertThrows(IllegalStateException.class, stream::openInputStream);
    }

    @Test
    void writeAndCancelledReadReleaseTheirOwnedSourceExactlyOnce() throws Exception {
        var closed = new AtomicInteger();
        var stream = new FileStream("image.png", "image/png", null, false,
                output -> output.write(1), () -> closed.incrementAndGet());
        stream.writeTo(new ByteArrayOutputStream());
        stream.close();
        assertEquals(1, closed.get());
        var cancelled = new FileStream("large.bin", "application/octet-stream", null, true,
                output -> output.write(new byte[256 * 1024]), () -> closed.incrementAndGet());
        try (var input = cancelled.openInputStream()) {
            input.read();
        }
        assertEquals(2, closed.get());
    }

    @Test
    void failureReleasesSourceAndKeepsOriginalFailure() {
        var closed = new AtomicInteger();
        var stream = new FileStream("image.png", "image/png", null, false,
                output -> {
                    throw new IOException("source failed");
                }, () -> closed.incrementAndGet());
        assertEquals("source failed", assertThrows(IOException.class,
                () -> stream.writeTo(new ByteArrayOutputStream())).getMessage());
        assertEquals(1, closed.get());
    }

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
