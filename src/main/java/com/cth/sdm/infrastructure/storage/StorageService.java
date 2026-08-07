package com.cth.sdm.infrastructure.storage;

import java.io.InputStream;

public interface StorageService {
    void store(String path, InputStream stream, long size, String contentType);
    InputStream load(String path);
    void delete(String path);
    boolean exists(String path);
}
