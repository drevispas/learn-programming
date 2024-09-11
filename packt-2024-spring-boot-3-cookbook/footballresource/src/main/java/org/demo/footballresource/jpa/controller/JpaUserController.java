package org.demo.footballresource.jpa.controller;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jpa.dto.JpaUser;
import org.demo.footballresource.jpa.service.JpaUserService;
import org.springframework.web.bind.annotation.*;

@RequiredArgsConstructor
@RequestMapping("/jpa/users")
@RestController
public class JpaUserController {

    private final JpaUserService jpaUserService;

    @PostMapping("")
    public JpaUser createUser(@RequestBody String name) {
        return jpaUserService.addUser(name);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Integer id) {
        jpaUserService.deleteUser(id);
    }

    @GetMapping("/{id}")
    public JpaUser getUser(@PathVariable Integer id) {
        return jpaUserService.readUser(id);
    }
}
