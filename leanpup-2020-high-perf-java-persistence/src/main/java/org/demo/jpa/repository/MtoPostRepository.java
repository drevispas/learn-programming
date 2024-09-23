package org.demo.jpa.repository;

import org.demo.jpa.entity.MtoPost;
import org.springframework.data.repository.CrudRepository;

public interface MtoPostRepository extends CrudRepository<MtoPost, Long> {
}
