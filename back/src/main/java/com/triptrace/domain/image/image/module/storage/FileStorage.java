package com.triptrace.domain.image.image.module.storage;

import com.triptrace.domain.image.image.module.dto.StoredFile;

import java.io.IOException;

public interface FileStorage {
    StoredFile save(byte[] file, String filePath, String fileName) throws IOException;
    void delete(String  filePath) throws IOException;
}
