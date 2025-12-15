package com.example.aichatbot.service.storage;

import java.io.InputStream;
import java.nio.file.Path;

public interface FileStorageService {
    String store(InputStream inputStream, String originalFilename);

    InputStream load(String filename);

    void delete(String filename);

    Path resolve(String filename);
}
