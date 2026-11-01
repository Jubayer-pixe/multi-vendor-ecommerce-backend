package com.abc.SpringBootSecqurityEx.service;

import java.io.IOException;
import java.io.InputStream;

/** Storage boundary for product image binaries. Storage keys are opaque and never filesystem paths. */
public interface ProductImageStorage {
    StagedImage stage(InputStream input) throws IOException;

    void promote(StagedImage image) throws IOException;

    InputStream open(String storageKey) throws IOException;

    StoredImage inspect(String storageKey) throws IOException;

    void delete(String storageKey) throws IOException;

    void discard(StagedImage image) throws IOException;

    record StagedImage(String stagingKey, String storageKey, String contentType, long sizeBytes,
                       int width, int height) { }

    record StoredImage(String contentType, long sizeBytes, int width, int height) { }
}
