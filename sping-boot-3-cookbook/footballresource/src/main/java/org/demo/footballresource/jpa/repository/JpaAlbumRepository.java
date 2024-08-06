package org.demo.footballresource.jpa.repository;

import org.demo.footballresource.jpa.entity.JpaAlbumEntity;
import org.demo.footballresource.jpa.entity.JpaPlayerEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

// Album -< Card >- Player >- Team
public interface JpaAlbumRepository extends JpaRepository<JpaAlbumEntity, Integer> {

    // List the players of an album. For instance, Pageable.ofSize(10).withPage(1)
    @Query("select p from JpaPlayerEntity p join p.cards c where c.album.id = :albumId")
    List<JpaPlayerEntity> findPageablePlayersByAlbumId(Integer albumId, Pageable pageable);

    // List the players of a certain team
//    @Query("select p from JpaPlayerEntity p join p.cards c where c.album.id = :albumId and p.team.id = :teamId")
    @Query("select p from JpaCardEntity c join c.player p where c.album.id = :albumId and p.team.id = :teamId")
    List<JpaPlayerEntity> findByAlbumIdAndTeamId(Integer albumId, Integer teamId);

    // List the players we don't have yet
    @Query("select p from JpaPlayerEntity p where p not in (select c.player from JpaCardEntity c where c.album.id = :albumId)")
    List<JpaPlayerEntity> findPlayersNotInAlbum(Integer albumId);
}
