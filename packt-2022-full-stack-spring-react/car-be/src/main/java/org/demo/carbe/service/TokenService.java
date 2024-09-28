package org.demo.carbe.service;

public interface TokenService {

    String generateToken(String username, String password);
}
