package org.demo.jpa.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Entity
public class BiOtmPost {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    String title;
    @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, mappedBy = "post")
    private List<BiOtmPostComment> comments = new ArrayList<>();

    public void addComment(BiOtmPostComment comment) {
        comments.add(comment);
        comment.setPost(this);
    }

    public void removeComment(BiOtmPostComment comment) {
        comments.remove(comment);
        comment.setPost(null);
    }
}
