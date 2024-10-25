package org.example.fpij.refactoring;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class LeapYearsTest {

  LeapYears leapYears;

  @BeforeEach
  void setUp() {
    leapYears = new LeapYears();
  }

  @Test
  void countFrom1900() {
    assertAll(
        () -> assertEquals(25, leapYears.countFrom1900(2000)),
        () -> assertEquals(27, leapYears.countFrom1900(2010)),
        () -> assertEquals(31, leapYears.countFrom1900(2025)),
        () -> assertEquals(49, leapYears.countFrom1900(2100)),
        () -> assertEquals(0, leapYears.countFrom1900(1800))
    );
  }
}
