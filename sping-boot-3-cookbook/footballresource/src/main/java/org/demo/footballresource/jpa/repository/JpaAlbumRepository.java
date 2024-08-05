package org.demo.footballresource.jpa.repository;

import org.demo.footballresource.jpa.entity.JpaAlbumEntity;
import org.demo.footballresource.jpa.entity.JpaPlayerEntity;
import org.demo.footballresource.jpa.entity.JpaTeamEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface JpaAlbumRepository extends JpaRepository<JpaAlbumEntity, Integer> {

    // List the players of a certain team
//    @Query("select p from JpaPlayerEntity p join p.cards c where c.album.id = :albumId and p.team.id = :teamId")
    @Query("select p from JpaAlbumEntity a join a.cards c join c.player p where a.id = :albumId and p.team.id = :teamId")
    public List<JpaPlayerEntity> findByAlbumIdAndTeamId(Integer albumId, Integer teamId);

    // List the players we don't have yet
    @Query("select p from JpaPlayerEntity p where p not in (select c.player from JpaCardEntity c where c.album.id = :albumId)")
    public List<JpaPlayerEntity> findPlayersNotInAlbum(Integer albumId);
}
