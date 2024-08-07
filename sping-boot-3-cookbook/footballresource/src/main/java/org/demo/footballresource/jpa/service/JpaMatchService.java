package org.demo.footballresource.jpa.service;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.jpa.dto.JpaPlayer;
import org.demo.footballresource.jpa.entity.JpaMatchEventDetails;
import org.demo.footballresource.jpa.entity.JpaMatchEventEntity;
import org.demo.footballresource.jpa.repository.JpaMatchEventRepository;
import org.demo.footballresource.jpa.repository.JpaMatchRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class JpaMatchService {

    private final JpaMatchRepository jpaMatchRepository;
    private final JpaMatchEventRepository jpaMatchEventRepository;

    public List<JpaPlayer> listMatchPlayers(int matchId) {
        return jpaMatchRepository.findMatchPlayers(matchId).stream()
                .map(it -> new JpaPlayer(it.getId(), it.getName(), it.getJerseyNumber(), it.getPosition(), it.getDateOfBirth()))
                .toList();
    }

    public List<JpaMatchEventDetails> listMatchEventsByMatchIdAndType(int matchId, int type) {
        // If tring to return JpaMatchEventEntity directly, it will cause a serialization error:
        //  "No serializer found for class org.hibernate.proxy.pojo.bytebuddy.ByteBuddyInterceptor and no properties discovered to create BeanSerializer"
        // This error happens when the entity uses Lazy fetch type and the entity is returned directly from the service method
        return jpaMatchEventRepository.findByIdIncludeType(matchId, type).stream()
                .map(JpaMatchEventEntity::getDetails)
                .toList();
    }
}
