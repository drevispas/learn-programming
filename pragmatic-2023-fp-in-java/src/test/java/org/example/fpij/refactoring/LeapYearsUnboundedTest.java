package org.example.fpij.refactoring;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LeapYearsUnboundedTest {

  LeapYearsUnbounded leapYearsUnbounded;

  @BeforeEach
  void setUp() {
    leapYearsUnbounded = new LeapYearsUnbounded();
  }

  @Test
  void countFrom1900() {
    assertAll(
        () -> assertEquals(25, leapYearsUnbounded.countFrom1900(y -> y <= 2000)),
        () -> assertEquals(27, leapYearsUnbounded.countFrom1900(y -> y <= 2010)),
        () -> assertEquals(31, leapYearsUnbounded.countFrom1900(y -> y <= 2025)),
        () -> assertEquals(49, leapYearsUnbounded.countFrom1900(y -> y <= 2100)),
        () -> assertEquals(0, leapYearsUnbounded.countFrom1900(y -> y <= 1800))
    );
  }
}
