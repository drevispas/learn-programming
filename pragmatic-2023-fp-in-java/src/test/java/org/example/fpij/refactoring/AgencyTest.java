package org.example.fpij.refactoring;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class AgencyTest {

  Agency agency;

  @BeforeEach
  void init() {
    agency = new Agency();
  }

  @Test
  void isChaperoneRequired() {
    Person jake=new Person("Jake",12);
    Person pam=new Person("Pam",14);
    Person shiv=new Person("Shiv",8);
    Person sam=new Person("Sam",9);
    Person jill=new Person("Jill",11);
    Person pam2=new Person("Pam",18);
    assertAll(
        () -> assertTrue(agency.isChaperoneRequired(Set.of(jake))),
        ()->assertTrue(agency.isChaperoneRequired(Set.of(jake, pam))),
        ()->assertTrue(agency.isChaperoneRequired(Set.of(jake, pam))),
        ()->assertTrue(agency.isChaperoneRequired(Set.of(shiv, sam, jill))),
        ()->assertFalse(agency.isChaperoneRequired(Set.of(jake, pam2))),
        ()->assertFalse(agency.isChaperoneRequired(Set.of()))
    );
  }
}
