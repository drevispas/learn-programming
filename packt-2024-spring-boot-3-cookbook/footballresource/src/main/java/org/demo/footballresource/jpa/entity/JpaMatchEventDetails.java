package org.demo.footballresource.jpa.entity;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class JpaMatchEventDetails {

    private Integer type;
    private String description;
    private List<Integer> players;
    private List<String> mediaFiles;
}
