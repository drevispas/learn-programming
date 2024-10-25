package org.example.fpij.refactoring;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FirstRepeatedLetterTest {

  FirstRepeatedLetter firstRepeatedLetter;

  @BeforeEach
  void init() {
    firstRepeatedLetter = new FirstRepeatedLetter();
  }

  @Test
  void findFirstRepeating(){
    assertAll(
        ()->assertEquals('l', firstRepeatedLetter.findIn("hello")),
        ()->assertEquals('h', firstRepeatedLetter.findIn("hellothere")),
        ()->assertEquals('a', firstRepeatedLetter.findIn("magicalguru")),
        ()->assertEquals('\0', firstRepeatedLetter.findIn("once")),
        ()->assertEquals('\0', firstRepeatedLetter.findIn(""))

    );
  }
}
