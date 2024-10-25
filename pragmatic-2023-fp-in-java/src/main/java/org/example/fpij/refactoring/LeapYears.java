package org.example.fpij.refactoring;

import java.time.Year;
import java.util.stream.IntStream;

public class LeapYears {

  public int countFrom1900(int upTo) {
    return (int) IntStream.iterate(1900, i->i<=upTo,i->i+4)
        .filter(Year::isLeap)
        .count();
  }
}
