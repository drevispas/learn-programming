package org.demo.jpa.repository;

import org.demo.jpa.entity.UniOtoNoFkPostDetail;
import org.demo.jpa.entity.UniOtoPost;
import org.demo.jpa.entity.UniOtoPostDetail;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @DataJpaTest 설정하고 @OneToMany 테스트하면 중간 테이블이 생성되지 않음
 */
@SpringBootTest
class UniOtoPostRepositoryTest {

    @Autowired
    private UniOtoPostRepository postRepository;
    @Autowired
    private UniOtoPostDetailRepository PostDetailRepository;
    @Autowired
    private UniOtoNoFkPostDetailRepository noFkPostDetailRepository;
//    @Autowired
//    private UniOtoWithJoinColumnPostRepository withJoinColumnPostRepository;

    private UniOtoPostDetail postDetail1;
    private UniOtoNoFkPostDetail postDetail2;
    private UniOtoPost post;
//    private UniOtoWithJoinColumnPost withJoinColumnPost;

    /**
     * Annotation이 안 붙은 테이블은 관계를 모름
     * insert into uni_oto_post (title,id) values (?,default)
     */
    @Test
    void savePost() {
        arrange();
        postRepository.save(post);
    }

    /**
     * Hibernate: insert into uni_oto_post (title,id) values (?,default) -> DB에 존재하면 insert하지 않음
     * Hibernate: insert into uni_oto_post_detail (created_by,created_on,post_id,id) values (?,?,?,default)
     */
    @Test
    void savePostDetail() {
        arrange();
        PostDetailRepository.save(postDetail1);
    }

    /**
     * @MapsId를 사용하면 부모의 PK를 자식의 PK로 사용함
     * Hibernate: insert into uni_oto_post (title,id) values (?,default)
     * Hibernate: insert into uni_oto_no_fk_post_detail (created_by,created_on,post_id) values (?,?,?)
     */
    @Test
    void saveWithNoFkPostDetail() {
        arrange();
        noFkPostDetailRepository.save(postDetail2);
    }

    private void arrange() {
        post = new UniOtoPost();
        post.setTitle("First post");
        postDetail1 = new UniOtoPostDetail();
        postDetail1.setCreatedBy("Kamil");
        postDetail1.setPost(post);
        postDetail2 = new UniOtoNoFkPostDetail();
        postDetail2.setCreatedBy("K");
        postDetail2.setPost(post);
    }
}
