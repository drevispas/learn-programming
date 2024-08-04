package org.demo.footballresource.nodb.service;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

// ApplicationRunner should be used to run the code right after the Spring Boot application is started and before ready to accept requests
// Once all the ApplicationRunner are executed the application is ready to accept requests that means the readiness probe is successful
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final FileLoader fileLoader;

    @Override
    public void run(ApplicationArguments args) {
        fileLoader.loadFile();
    }
}
