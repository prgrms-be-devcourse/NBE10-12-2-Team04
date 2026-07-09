package com.triptrace.domain.image.image.storage;

import com.triptrace.domain.image.image.processing.dto.StoredFile;

import java.io.IOException;

public interface FileStorage {
    StoredFile save(byte[] file, String filePath, String fileName) throws IOException;
    void delete(String  filePath) throws IOException;
}
