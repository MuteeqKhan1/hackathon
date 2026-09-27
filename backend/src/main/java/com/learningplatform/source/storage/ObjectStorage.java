package com.learningplatform.source.storage;

import java.io.InputStream;

public interface ObjectStorage {

    void put(String key, InputStream content, long contentLength, String contentType);

    InputStream get(String key);

    boolean exists(String key);
}
