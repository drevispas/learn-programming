package org.demo.footballresource.jpa.service;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jpa.dto.JpaUser;
import org.demo.footballresource.jpa.entity.JpaUserEntity;
import org.demo.footballresource.jpa.repository.JpaUserRepository;
import org.springframework.stereotype.Service;

import java.util.Optional;

@RequiredArgsConstructor
@Service
public class JpaUserService {

    private final JpaUserRepository jpaUserRepository;

    public JpaUser addUser(String name) {
        JpaUserEntity user = jpaUserRepository.save(new JpaUserEntity(name));
        return new JpaUser(user.getId(), user.getUsername());
    }

    public void deleteUser(Integer id) {
        jpaUserRepository.deleteById(id);
    }

    public JpaUser readUser(Integer id) {
        Optional<JpaUserEntity> user = jpaUserRepository.findById(id);
        if (user.isEmpty()) {
            throw new IllegalArgumentException("User not found");
        }
        return new JpaUser(user.get().getId(), user.get().getUsername());
    }
}
