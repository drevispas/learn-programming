package org.demo.footballresource.actuator;

import org.demo.footballresource.nodb.service.FileLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ActuatorConfig {

    @Bean
    public FootballCustomerEndpoint footballCustomerEndpoint(FileLoader fileLoader) {
        return new FootballCustomerEndpoint(fileLoader);
    }
}
