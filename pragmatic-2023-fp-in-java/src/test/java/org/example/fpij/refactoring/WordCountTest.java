package org.example.fpij.refactoring;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WordCountTest {

  WordCount wordCount;

  @BeforeEach
  void init() {
    wordCount = new WordCount();
  }

  @Test
  void count() {
    assertAll(
        ()->assertEquals(2, wordCount.countInFile("public", "WordCount.java")),
        ()->assertEquals(1, wordCount.countInFile("package", "WordCount.java"))
    );
  }
}
