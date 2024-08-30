package org.demo.footballresource.mongo.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.demo.footballresource.mongo.dto.MongoPlayer;
import org.demo.footballresource.mongo.entity.MongoTeam1;
import org.demo.footballresource.mongo.repository.MongoTeamRepository;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Service
public class MongoFootballService {

    private final MongoTeamRepository teamRepository;
    private final MongoTemplate mongoTemplate;

    public List<MongoTeam1> listAllTeams() {
        return teamRepository.findAll();
    }

    public MongoTeam1 getTeam(String id) {
        return teamRepository.findById(id).orElse(null);
    }

    public MongoTeam1 searchTeamByName(String teamName) {
        return teamRepository.findByName(teamName).orElse(null);
    }

    public List<MongoTeam1> getTeamsContainingName(String name) {
        return teamRepository.findByNameContaining(name);
    }

    public List<MongoTeam1> listTeamsByNameSQL(String name) {
        return teamRepository.findTeamsByNameSQL(name);
    }

    public MongoPlayer getPlayer(String id) {
        return teamRepository.findPlayerById(id)
                .map(team -> {
                    log.info("Team: {}", team);
                    return team.getPlayers().getFirst();
                })
                .orElse(null);
    }

    public MongoTeam1 createTeam(MongoTeam1 team) {
        return teamRepository.save(team);
    }

    public void deleteTeam(String id) {
        teamRepository.deleteById(id);
    }

    public void updateTeamName(String teamId, String newName) {
        Query query = new Query(Criteria.where("id").is(teamId));
        Update update = new Update().set("name", newName);
        mongoTemplate.updateFirst(query, update, MongoTeam1.class);
    }
}
