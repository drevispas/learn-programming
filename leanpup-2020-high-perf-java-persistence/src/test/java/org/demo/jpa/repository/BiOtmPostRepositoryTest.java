package org.demo.jpa.repository;

import org.demo.jpa.entity.BiOtmPost;
import org.demo.jpa.entity.BiOtmPostComment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @DataJpaTest 설정하고 @OneToMany 테스트하면 중간 테이블이 생성되지 않음
 */
@SpringBootTest
class BiOtmPostRepositoryTest {

    @Autowired
    private BiOtmPostRepository postRepository;
    @Autowired
    private BiOtmPostCommentRepository postCommentRepository;

    private BiOtmPostComment postComment1;
    private BiOtmPostComment postComment2;
    private BiOtmPost post;

    /**
     * 기대하는 대로 동작함
     * Hibernate: insert into bi_otm_post (title,id) values (?,default)
     * Hibernate: insert into bi_otm_post_comment (post_id,review,id) values (?,?,default)
     * Hibernate: insert into bi_otm_post_comment (post_id,review,id) values (?,?,default)
     */
    @Test
    void savePost() {
        arrange();
        postRepository.save(post);
    }

    /**
     * comment1만 저장하려고 해도 부모 및 형제까지 모두 저장됨
     * Hibernate: insert into bi_otm_post (title,id) values (?,default)
     * Hibernate: insert into bi_otm_post_comment (post_id,review,id) values (?,?,default)
     * Hibernate: insert into bi_otm_post_comment (post_id,review,id) values (?,?,default)
     */
    @Test
    void savePostComment() {
        arrange();
        postCommentRepository.save(postComment1);
    }

    private void arrange() {
        postComment1 = new BiOtmPostComment();
        postComment1.setReview("Great post");
        postComment2 = new BiOtmPostComment();
        postComment2.setReview("Nice post");

        post = new BiOtmPost();
        post.setTitle("First post");
        post.addComment(postComment1);
        post.addComment(postComment2);
    }
}
