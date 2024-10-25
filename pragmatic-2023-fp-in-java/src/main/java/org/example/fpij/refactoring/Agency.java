package org.example.fpij.refactoring;

import java.util.Set;

public class Agency {

  public boolean isChaperoneRequired(Set<Person> people) {
//    boolean required=true;
//    if (people.isEmpty()) required=false;
//    else {
//      for (var person:people) {
//        if (person.age() >= 18) {
//          required=false;
//          break;
//        }
//      }
//    }
//    return required;

    return !people.isEmpty() && people.stream().noneMatch(p -> p.age()>=18);
  }
}
