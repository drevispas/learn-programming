package org.demo.jpa.repository;

import org.demo.jpa.entity.UniOtmPost;
import org.demo.jpa.entity.UniOtmPostComment;
import org.demo.jpa.entity.UniOtmWithJoinColumnPost;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * @DataJpaTest 설정하고 @OneToMany 테스트하면 중간 테이블이 생성되지 않음
 */
@SpringBootTest
class UniOtmPostRepositoryTest {

    @Autowired
    private UniOtmPostRepository postRepository;
    @Autowired
    private UniOtmPostCommentRepository postCommentRepository;
    @Autowired
    private UniOtmWithJoinColumnPostRepository withJoinColumnPostRepository;

    private UniOtmPostComment postComment1;
    private UniOtmPostComment postComment2;
    private UniOtmPost post;
    private UniOtmWithJoinColumnPost withJoinColumnPost;

    /**
     * Unidirectional OneToMany는 junction table을 사용하는 문제가 있음
     * Hibernate: insert into uni_otm_post (title,id) values (?,default)
     * Hibernate: insert into uni_otm_post_comment (review,id) values (?,default)
     * Hibernate: insert into uni_otm_post_comment (review,id) values (?,default)
     * Hibernate: insert into uni_otm_post_comments (uni_otm_post_id,comments_id) values (?,?)
     * Hibernate: insert into uni_otm_post_comments (uni_otm_post_id,comments_id) values (?,?)
     */
    @Test
    void savePost() {
        arrange();
        postRepository.save(post);
    }

    /**
     * Annotaion이 안 붙은 테이블은 관계를 모름
     * Hibernate: insert into uni_otm_post_comment (review,id) values (?,default)
     */
    @Test
    void savePostComment() {
        arrange();
        postCommentRepository.save(postComment1);
    }

    /**
     * @JoinColumn을 사용하면 junction table을 사용하지 않는 대신에 FK를 업데이트 함
     * Hibernate: insert into uni_otm_with_join_column_post (title,id) values (?,default)
     * Hibernate: insert into uni_otm_post_comment (review,id) values (?,default)
     * Hibernate: insert into uni_otm_post_comment (review,id) values (?,default)
     * Hibernate: update uni_otm_post_comment set post_id=? where id=?
     * Hibernate: update uni_otm_post_comment set post_id=? where id=?
     */
    @Test
    void saveWithJoinColumnPost() {
        arrange();
        withJoinColumnPostRepository.save(withJoinColumnPost);
    }

    private void arrange() {
        postComment1 = new UniOtmPostComment();
        postComment1.setReview("Great post");
        postComment2 = new UniOtmPostComment();
        postComment2.setReview("Nice post");

        post = new UniOtmPost();
        post.setTitle("First post");
        post.getComments().add(postComment1);
        post.getComments().add(postComment2);

        withJoinColumnPost = new UniOtmWithJoinColumnPost();
        withJoinColumnPost.setTitle("Second post");
        withJoinColumnPost.getComments().add(postComment1);
        withJoinColumnPost.getComments().add(postComment2);
    }
}
