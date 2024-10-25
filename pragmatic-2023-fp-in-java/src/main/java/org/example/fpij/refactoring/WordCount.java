package org.example.fpij.refactoring;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.stream.Stream;

public class WordCount {

  public int countInFile(String keyword, String filePath) throws IOException, URISyntaxException {
//    BufferedReader br = new BufferedReader(new FileReader(filePath));
//    int count = 0;
//    String line = null;
//    while ((line = br.readLine()) != null) {
//      String[] words = line.split(" ");
//      for (String w: words) {
//        if(w.equals(keyword)) count++;
//      }
//    }
//    return count;

    return (int) Files
        .lines(Paths.get(filePath))
        .flatMap(l -> Stream.of(l.split(" ")))
        .peek(System.out::println)
        .filter(w -> w.equals(keyword))
        .count();
  }
}
