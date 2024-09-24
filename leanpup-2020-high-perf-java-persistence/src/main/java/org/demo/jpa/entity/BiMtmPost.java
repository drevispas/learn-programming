package org.demo.jpa.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashSet;
import java.util.Set;

@Data
@NoArgsConstructor
@Entity
public class BiMtmPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    String title;
    // post 테이블에 정보가 더 풍부해서 owning table로 설정
    @ManyToMany(cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinTable(name = "bi_mtm_post_tag",
            joinColumns = @JoinColumn(name = "tag_id"),
            inverseJoinColumns = @JoinColumn(name = "post_id"))
    private Set<BiMtmTag> tags = new HashSet<>();

    // helper methods
    public void addTag(BiMtmTag tag) {
        tags.add(tag);
        tag.getPosts().add(this);
    }

    public void removeTag(BiMtmTag tag) {
        tags.remove(tag);
        tag.getPosts().remove(this);
    }
}
