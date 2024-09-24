package org.demo.jpa.repository;

import org.demo.jpa.entity.BiMtmPost;
import org.demo.jpa.entity.BiMtmTag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @DataJpaTest 설정하고 @OneToMany 테스트하면 중간 테이블이 생성되지 않음
 */
@SpringBootTest
class BiMtmPostRepositoryTest {

    @Autowired
    private BiMtmPostRepository postRepository;
    @Autowired
    private BiMtmTagRepository TagRepository;

    private BiMtmPost post1, post2;
    private BiMtmTag tag1, tag2;

    /**
     * Hibernate: insert into bi_mtm_post (title,id) values (?,default)
     * Hibernate: insert into bi_mtm_tag (name,id) values (?,default)
     * Hibernate: insert into bi_mtm_tag (name,id) values (?,default)
     * Hibernate: insert into bi_mtm_post_tag (tag_id,post_id) values (?,?)
     * Hibernate: insert into bi_mtm_post_tag (tag_id,post_id) values (?,?)
     */
    @Test
    void savePost() {
        arrange();
        postRepository.save(post1);
    }

    /**
     * Hibernate: insert into bi_mtm_tag (name,id) values (?,default)
     */
    @Test
    void saveTag() {
        arrange();
        TagRepository.save(tag1);
    }

    private void arrange() {
        post1 = new BiMtmPost();
        post1.setTitle("First post");
        post2 = new BiMtmPost();
        post2.setTitle("Second post");
        tag1 = new BiMtmTag();
        tag1.setName("Java");
        tag2 = new BiMtmTag();
        tag2.setName("Spring");
        post1.getTags().add(tag1);
        post1.getTags().add(tag2);
        post2.getTags().add(tag1);
    }
}
