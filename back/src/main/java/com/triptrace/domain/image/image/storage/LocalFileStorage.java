package com.triptrace.domain.image.image.storage;


import com.triptrace.domain.image.image.processing.dto.StoredFile;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Component
public class LocalFileStorage implements FileStorage {
    @Override
    public StoredFile save(byte[] file, String filePath, String fileName) throws IOException {
        Path path = Paths.get(filePath);
        Files.createDirectories(path);
        Path outputPath = path.resolve(fileName);
        Files.write(outputPath, file);
        return new StoredFile(filePath, fileName, (long) file.length);
    }
    @Override
    public void delete(String filePath) throws IOException {
        Files.deleteIfExists(Paths.get(filePath));
    }
}
