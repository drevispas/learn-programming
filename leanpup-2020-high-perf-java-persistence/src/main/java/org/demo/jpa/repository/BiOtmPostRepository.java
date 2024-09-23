package org.demo.jpa.repository;

import org.demo.jpa.entity.BiOtmPost;
import org.demo.jpa.entity.UniOtmPost;
import org.springframework.data.repository.CrudRepository;

public interface BiOtmPostRepository extends CrudRepository<BiOtmPost, Long> {
}
