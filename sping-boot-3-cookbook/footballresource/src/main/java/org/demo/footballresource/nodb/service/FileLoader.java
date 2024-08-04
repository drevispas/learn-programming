package org.demo.footballresource.nodb.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileLoader {

    private final String folder;
    @Getter
    private String fileName;
    @Getter
    private List<String> teams;

    private void loadFile(String fileName) throws IOException {
        this.fileName = fileName;
        // Load file content
        ObjectMapper mapper = new ObjectMapper();
        File file = new File(folder + "/" + fileName);
        teams = mapper.readValue(file, new TypeReference<>() {
        });
    }

    public void loadFile() {
        try {
            Thread.sleep(5000);
            Files.list(Paths.get(folder))
                    .filter(Files::isRegularFile)
                    .filter(file -> file.toString().endsWith(".json"))
                    .peek(file -> log.info("Filtered file: {}", file))
                    .findFirst()
                    .ifPresent(file -> {
                        try {
                            loadFile(file.getFileName().toString());
                        } catch (IOException e) {
                            log.error("Error loading file", e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException("Error loading file", e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }
}
