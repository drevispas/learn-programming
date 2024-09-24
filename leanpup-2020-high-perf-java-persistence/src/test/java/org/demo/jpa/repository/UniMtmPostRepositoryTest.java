package org.demo.jpa.repository;

import org.demo.jpa.entity.UniMtmPost;
import org.demo.jpa.entity.UniMtmTag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @DataJpaTest 설정하고 @OneToMany 테스트하면 중간 테이블이 생성되지 않음
 */
@SpringBootTest
class UniMtmPostRepositoryTest {

    @Autowired
    private UniMtmPostRepository postRepository;
    @Autowired
    private UniMtmTagRepository TagRepository;

    private UniMtmPost post1, post2;
    private UniMtmTag tag1, tag2;

    /**
     * Hibernate: insert into uni_mtm_post (title,id) values (?,default)
     * Hibernate: insert into uni_mtm_tag (name,id) values (?,default)
     * Hibernate: insert into uni_mtm_tag (name,id) values (?,default)
     * Hibernate: insert into uni_mtm_post_tag (tag_id,post_id) values (?,?)
     * Hibernate: insert into uni_mtm_post_tag (tag_id,post_id) values (?,?)
     */
    @Test
    void savePost() {
        arrange();
        postRepository.save(post1);
    }

    /**
     * Hibernate: insert into uni_mtm_tag (name,id) values (?,default)
     */
    @Test
    void saveTag() {
        arrange();
        TagRepository.save(tag1);
    }

    private void arrange() {
        post1 = new UniMtmPost();
        post1.setTitle("First post");
        post2 = new UniMtmPost();
        post2.setTitle("Second post");
        tag1 = new UniMtmTag();
        tag1.setName("Java");
        tag2 = new UniMtmTag();
        tag2.setName("Spring");
        post1.getTags().add(tag1);
        post1.getTags().add(tag2);
        post2.getTags().add(tag1);
    }
}
