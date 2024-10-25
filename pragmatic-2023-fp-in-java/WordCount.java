package org.example.fpij.refactoring;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;

public class WordCount {

  public int countInFile(String word, String filePath) throws FileNotFoundException {
    BufferedReader br = new BufferedReader(new FileReader(filePath));
    return 0;
  }
}
