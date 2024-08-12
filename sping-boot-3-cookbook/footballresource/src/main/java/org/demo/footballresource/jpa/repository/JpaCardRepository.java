package org.demo.footballresource.jpa.repository;

import org.demo.footballresource.jpa.entity.JpaCardEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface JpaCardRepository extends JpaRepository<JpaCardEntity, Integer> {

    @Modifying
    @Query(nativeQuery = true, value= """
    -- distribute all cards that are not in any album to an album that is not full
    update cards set album_id = r.album_id
    from (
        -- find all albums that are not full
        select available.album_id,
               -- find a card that is not in any album
               (select c2.id
                from cards c2
                -- filter the first available card that is not in any album
                where c2.owner_id = ?1 and c2.player_id = available.player_id and c2.album_id is null limit 1
               ) as card_id
        from (
            -- find all albums that are not full
            select distinct a.id as album_id, c.player_id
            from albums a cross join cards c
            -- find all cards that are not in any album
            where a.owner_id = ?1 and c.owner_id = ?1 and c.album_id is null
                -- filter out players that are already in the album
                and c.player_id not in (
                    select uc.player_id from cards uc where uc.album_id = a.id
                )
        ) as available
    ) as r
    where cards.id = r.card_id
    returning cards.*
    """)
    List<JpaCardEntity> distributeUnusedCardsToAlbum(int userId);

    @Modifying
    @Query("""
    update JpaCardEntity set
        album = null,
        owner = (select u from JpaCardEntity u where u.id = ?2)
    where id = ?1
    """)
    int transferCard(int cardId, int userId);

    // `@Modifying` make sure that the method flushes the changes to the database
    // With `returning` clause, we can return the updated cards instead of the number of updated cards
    @Modifying
    @Query(nativeQuery = true, value= """
    update cards set owner_id = ?2
    from (
        -- find all unused cards of user1 that can be transferred to user2 because user2 does not have them
        select c1.id
        from cards c1
        where c1.owner_id = ?1 and c1.album_id is null
            and c1.player_id in (
                select p2.id
                from players p2
                where p2.id not in (
                    select c2.player_id from cards c2 where c2.owner_id = ?2
                )
            )
        limit ?3
    ) cards_from_user1_for_user2
    where cards.id = cards_from_user1_for_user2.id
    returning cards.*
    """)
    List<JpaCardEntity> transferCardsBetweenUsers(int userId1, int userId2, int count);

    // count the number of cards that are not in any album and are not in the target user's collection
    @Query("""
    select count(c)
    from JpaCardEntity c
    where c.owner.id = ?1 and c.album = null
        and c.player not in (
            select c2.player
            from JpaCardEntity c2
            where c2.owner.id = ?2
        )
    """)
    int countMatchBetweenUsers(int userId1, int userId2);
}
