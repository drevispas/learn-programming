package org.demo.footballresource.actuator;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.service.FileLoader;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

// @Endpoint annotation is used to create a custom Actuator endpoint
// All methods annotated with @ReadOperation, @WriteOperation, or @DeleteOperation will be exposed over JMX and HTTP
@Endpoint(id = "football")
@RequiredArgsConstructor
public class FootballCustomerEndpoint {

    private final FileLoader fileLoader;

    @ReadOperation
    public String getFileName() {
        return fileLoader.getFileName();
    }

    @WriteOperation
    public void reloadFile() {
        fileLoader.loadFile();
    }
}
