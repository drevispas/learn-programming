package org.demo.album;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@EnableDiscoveryClient
// @EnableFeighClients scans the interfaces annotated with @FeignClien to create a Feign client.
@EnableFeignClients
@SpringBootApplication
public class AlbumApplication {

    public static void main(String[] args) {
        SpringApplication.run(AlbumApplication.class, args);
    }

}
