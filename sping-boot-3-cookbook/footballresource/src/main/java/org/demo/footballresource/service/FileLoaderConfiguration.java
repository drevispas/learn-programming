package org.demo.footballresource.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FileLoaderConfiguration {

    @Value("${football.folder}")
    private String folder;

    @Bean
    public FileLoader fileLoader() {
        return new FileLoader(folder);
    }

}
