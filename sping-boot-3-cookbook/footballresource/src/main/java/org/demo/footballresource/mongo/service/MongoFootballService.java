package org.demo.footballresource.mongo.service;

import lombok.RequiredArgsConstructor;
import org.demo.footballresource.mongo.entity.MongoMatchEvent;
import org.demo.footballresource.mongo.entity.MongoPlayer;
import org.demo.footballresource.mongo.entity.MongoTeam;
import org.demo.footballresource.mongo.repository.MongoMatchEventRepository;
import org.demo.footballresource.mongo.repository.MongoMatchRepository;
import org.demo.footballresource.mongo.repository.MongoPlayerRepository;
import org.demo.footballresource.mongo.repository.MongoTeamRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class MongoFootballService {

    private final MongoTeamRepository mongoTeamRepository;
    private final MongoPlayerRepository mongoPlayerRepository;
    private final MongoMatchRepository mongoMatchRepository;
    private final MongoMatchEventRepository mongoMatchEventRepository;
    private final MongoTemplate mongoTemplate;

    public MongoTeam getTeamById(String id) {
        return mongoTeamRepository.findById(id).orElse(null);
    }

    public MongoTeam getTeamByName(String name) {
        return mongoTeamRepository.findByName(name).orElse(null);
    }

    public List<MongoTeam> listTeamsByName(String name) {
        return mongoTeamRepository.findByNameContaining(name);
    }

    public MongoTeam saveTeam(MongoTeam team) {
        return mongoTeamRepository.save(team);
    }

    public void deleteTeam(String id) {
        mongoTeamRepository.deleteById(id);
    }

    public void updateTeamName(String id, String name) {
        Query query = new Query(Criteria.where("id").is(id));
        Update update = new Update().set("name", name);
        mongoTemplate.updateFirst(query, update, MongoTeam.class);
    }

    public MongoPlayer getPlayerById(String id) {
        return mongoPlayerRepository.findById(id).orElse(null);
    }

    public List<MongoMatchEvent> listMatchEvents(String matchId) {
        return mongoMatchEventRepository.findByMatchId(matchId);
    }

    public List<MongoMatchEvent> listPlayerEvents(String matchId, String playerId) {
        return mongoMatchEventRepository.findByMatchIdAndPlayerId(matchId, playerId);
    }
}
