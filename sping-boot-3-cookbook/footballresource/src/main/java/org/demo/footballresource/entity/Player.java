package org.demo.footballresource.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.time.LocalDate;

@RequiredArgsConstructor
@AllArgsConstructor
@Data
public class Player {

    private int id;
    private int jerseyNumber;
    private String name;
    private String position;
    private LocalDate dateOfBirth;
    private int teamId;
}
