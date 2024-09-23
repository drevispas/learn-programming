package org.demo.jpa.repository;

import org.demo.jpa.entity.MtoPostComment;
import org.demo.jpa.entity.MtoPost;
import org.demo.jpa.entity.MtoWithJoinColumnPostComment;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class MtoPostRepositoryTest {

    @Autowired
    private MtoWithJoinColumnPostCommentRepository postCommentRepository;
    @Autowired
    private MtoPostCommentRepository noJoinColumnPostCommentRepository;
    @Autowired
    private MtoPostRepository postRepository;

    private MtoPost withJoinColumnPost;
    private MtoWithJoinColumnPostComment withJoinColumnPostComment;

    private MtoPost post;
    private MtoPostComment postComment;

    /**
     * 자식을 insert하기 전에 부모가 insert됨
     * Hibernate: insert into mto_post (title,id) values (?,default)
     * Hibernate: insert into mto_post_comment (post_id,review,id) values (?,?,default)
     */
    @Test
    void savePostComment() {
        arrange();
        noJoinColumnPostCommentRepository.save(postComment);
    }

    /**
     * Annotation이 안 붙은 테이블은 관계를 모름
     * 부모를 insert해도 자식은 insert되지 않음
     * Hibernate: insert into mto_post (title,id) values (?,default)
     */
    @Test
    void savePost() {
        arrange();
        postRepository.save(post);
    }

    /**
     * @JoinColumn 있어도 동일함
     * Hibernate: insert into mto_post (title,id) values (?,default)
     * Hibernate: insert into mto_with_join_column_post_comment (post_id,review,id) values (?,?,default)
     */
    @Test
    void saveWithJoinColumnPostComment() {
        arrange();
        postCommentRepository.save(withJoinColumnPostComment);
    }

    /**
     * @JoinColumn 있어도 동일함
     * Hibernate: insert into mto_post (title,id) values (?,default)
     */
    @Test
    void saveWithJoinColumnPost() {
        arrange();
        postRepository.save(withJoinColumnPost);
    }

    private void arrange() {
        post = new MtoPost();
        post.setTitle("Post 2");
        postComment = new MtoPostComment();
        postComment.setReview("Post 2 review");
        postComment.setPost(post);

        withJoinColumnPost = new MtoPost();
        withJoinColumnPost.setTitle("Post 1");
        withJoinColumnPostComment = new MtoWithJoinColumnPostComment();
        withJoinColumnPostComment.setReview("Post 1 review");
        withJoinColumnPostComment.setPost(withJoinColumnPost);
    }
}
