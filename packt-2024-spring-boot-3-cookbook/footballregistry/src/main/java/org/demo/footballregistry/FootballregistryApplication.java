package org.demo.footballregistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@EnableEurekaServer
@SpringBootApplication
public class FootballregistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(FootballregistryApplication.class, args);
    }

}
