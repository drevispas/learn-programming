package org.demo.jpa.repository;

import org.demo.jpa.entity.BiMtmPost;
import org.springframework.data.repository.CrudRepository;

public interface BiMtmPostRepository extends CrudRepository<BiMtmPost, Long> {
}
