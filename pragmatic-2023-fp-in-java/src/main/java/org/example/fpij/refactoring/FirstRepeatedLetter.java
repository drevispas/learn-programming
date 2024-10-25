package org.example.fpij.refactoring;

import java.util.Arrays;
import java.util.stream.IntStream;

public class FirstRepeatedLetter {

  public char findIn(String word) {
//    // imperative version
//    char[] letters = word.toCharArray();
//    for (int i=0; i<letters.length-1; i++) {
//      char letter = letters[i];
//      for (int j=i+1; j<letters.length; j++) {
//        if (letters[j] == letter) return letter;
//      }
//    }
//    return '\0';

    // functional version
    return Arrays
        .stream(word.split(""))
        .filter(l -> word.lastIndexOf(l) > word.indexOf(l))
        .findFirst()
        .map(s -> s.charAt(0))
        .orElse('\0');
  }
}
