package org.example.fpij.refactoring;

import java.time.Year;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

//interface Continue {
//
//  Boolean check(int year);
//}

public class LeapYearsUnbounded {

  public int countFrom1900(Predicate<Integer> shouldContinue) {
//    int numberOfLeapYears = 0;
//
//    for(int i=1900;;i+=4){
//      if(!shouldContinue.check(i)) break;
//      if(Year.isLeap(i)) numberOfLeapYears++;
//    }
//    return numberOfLeapYears;

    return (int) Stream.iterate(1900, x -> x+4)
        .takeWhile(shouldContinue::test)
        .filter(Year::isLeap)
        .count();
  }
}
