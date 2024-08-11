package org.demo.footballresource.jpa.repository;

import org.demo.footballresource.jpa.entity.JpaUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface JpaUserRepository extends JpaRepository<JpaUserEntity, Integer> {

    @Query("""
    select u
    from JpaUserEntity u
    join fetch u.ownedCards c
    join fetch u.ownedAlbums a
    join fetch c.player p
    where u.id = ?1
    """)
    public JpaUserEntity findUserWithCardsAndAlbums(int userId);
}
