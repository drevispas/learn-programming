package org.demo.football.controller;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/serviceinfo")
@RestController
public class ServiceInformationController {

    @Value("${football.instance-id}")
    private String instanceId;

    @GetMapping
    public String getInstanceId() {
        return instanceId;
    }
}
