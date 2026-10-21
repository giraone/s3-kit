package com.giraone.s3kit.download;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Result of an S3 download. The content is streamed via {@link #inputStream()} and never held completely in memory.
 * The instance <b>must be closed</b> by the caller to release the underlying HTTP connection.
 *
 * @param bucket        the bucket name
 * @param key           the S3 key
 * @param status        the HTTP status code of the response
 * @param contentType   the content type, or {@code null} if unknown
 * @param contentLength the content length in bytes, or {@code null} if unknown
 * @param inputStream   the stream of the object content
 */
public record S3Download(
    String bucket, String key, int status, String contentType, Long contentLength, InputStream inputStream)
    implements Closeable {

    /**
     * Copies the complete content to the given stream and closes this download.
     *
     * @param out the target
     * @return the number of bytes copied
     * @throws IOException on read or write errors
     */
    public long transferTo(final OutputStream out) throws IOException {
        try (InputStream in = inputStream) {
            return in.transferTo(out);
        }
    }

    /**
     * The last segment of the key.
     */
    public String fileName() {
        final int i = key.lastIndexOf('/');
        return i > 0 ? key.substring(i + 1) : key;
    }

    @Override
    public void close() throws IOException {
        inputStream.close();
    }
}
