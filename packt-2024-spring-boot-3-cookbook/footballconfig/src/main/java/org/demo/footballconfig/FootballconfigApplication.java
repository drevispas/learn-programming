package org.demo.footballconfig;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.config.server.EnableConfigServer;

@EnableConfigServer
@SpringBootApplication
public class FootballconfigApplication {

    public static void main(String[] args) {
        SpringApplication.run(FootballconfigApplication.class, args);
    }

}
