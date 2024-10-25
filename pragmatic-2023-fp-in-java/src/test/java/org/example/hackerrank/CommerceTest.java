package org.example.hackerrank;

import com.sun.source.tree.Tree;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

public class CommerceTest {

  @Test
  void teset1() {
//    List<String> words=List.of("aba", "ba", "bbba", "bc", "cccb", "abc");
//    List<String> words=List.of("a","b","c");
//    List<String> words=List.of("aba", "abaca", "baab", "cba");
    List<String> words = List.of("aba", "aba", "ba", "ab");
    countSimilarPairs(words);
  }

    /*
  Two strings are said to be similar if they are composed of the same characters.
  For example "abaca" and "cba" are similar since both of them are composed of characters 'a', 'b' and 'c'.
  However "abaca" and "bcd" are not similar since they do not share all of the same letters.

  Given an array of strings words of length n, find the number of pairs of strings that are similar.

  Note:
  - Each string is composed of lowercase English characters only.
  - Pairs are considered index-wise, i.e., two equal strings at different indices are counted as separated pairs.
  - A pair at indices (i, j) is the same as the pair at (j, i).

  Example
  Consider n = 3, words = ["xyz", "foo", "of"].
  Here, the strings "foo" and "of" are similar because they are composed of the same characters ['o', 'f'].
  There are no other similar pairs so the answer is 1.
   */
  public long countSimilarPairs(List<String> words) {
    // Create a map to count occurrences of each unique word pattern
    Map<String, Long> wordCount = new HashMap<>();
    for (String s : words) {
      // Convert the word to a sorted set of characters to create a unique pattern
      Set<Character> compressedWord = s.chars()
          // IntStream -> Stream<Character>
          .mapToObj(i -> (char) i).sorted()
          .collect(Collectors.toSet());
      // Set<Character> -> String
      // Convert the set of characters back to a string to use as a key
      String key = compressedWord.stream().map(String::valueOf).collect(Collectors.joining());
      // Increment the count for this pattern in the map
      wordCount.put(key, wordCount.getOrDefault(key, 0L) + 1);
    }
    System.out.println(wordCount);
    // Calculate the number of similar pairs using the formula n * (n - 1) / 2
    long numPairs = wordCount.values().stream().reduce(0L, (p, q) -> p + q * (q - 1) / 2);
    System.out.println(numPairs);
    return numPairs;
  }

  @Test
  void test2() {
    List<Integer> list1 = List.of(1, 2, 3, 123322020);
    List<Integer> list2 = List.of(3, 3, 5, 123322020);
    getMinCores(list1, list2);
  }

  /*
  Process scheduling algorithms are used by a CPU to optimally schedule the running processes.
  A core can execute one process at a time, but a CPU may have multiple cores.

  There are n processes where the ith process starts its execution at start[i] and ends at end[i], both inclusive.
  Find the minimum number of cores required to execute the processes.

  Example
  n = 3
  start = [1, 3, 4]
  end = [3, 5, 6].

  If the CPU has only one core, the first process starts at 1 and ends at 3.
  The second process starts at 3. Since both processes need a processor at 3, they overlap.
  There must be more than 1 core.

  If the CPU has two cores, the first process runs on the first core from 1 to 3,
  the second runs on the second core from 3 to 5, and the third process runs on the first core from 4 to 6.

  Return 2, the minimum number of cores required.
   */
  int getMinCores(List<Integer> start, List<Integer> end) {
//    int n = end.stream().mapToInt(v->v).max().orElse(0)+1;
//    List<Integer> requiredCores = new ArrayList<>(n);
//    for(int i=0;i<=n;i++) requiredCores.add(0);
//    for(int i=0;i<start.size();i++){
//      int startTime=start.get(i), endTime=end.get(i);
//      requiredCores.set(startTime, requiredCores.get(startTime)+1);
//      if (endTime+1 <= n)
//        requiredCores.set(endTime+1, requiredCores.get(endTime+1)-1);
//    }
//    int maxCores=0, cores=0;
//    for(int i=1;i<=n;i++){
//      cores += requiredCores.get(i);
//      maxCores = Math.max(maxCores, cores);
//    }
//    return maxCores;
//  }

    // Create a map to count the number of jobs starting and ending at each time
    Map<Integer, Integer> jobCount = new HashMap<>();
    for (int s : start) {
      // Increment the count for the start time
      jobCount.put(s, jobCount.getOrDefault(s, 0) + 1);
    }
    for (int e : end) {
      // Decrement the count for the time after the end time
      jobCount.put(e + 1, jobCount.getOrDefault(e + 1, 0) - 1);
    }
    System.out.println("jobCount=" + jobCount);
    // Create a list of core requirements sorted by time
    List<Integer> spotCores = jobCount.entrySet().stream()
        .sorted(Comparator.comparingInt(Map.Entry::getKey))
        .map(x -> x.getValue())
        .collect(Collectors.toList());
    System.out.println("spotCores=" + spotCores);
    int maxCores = 0, acc = 0;
    for (int c : spotCores) {
      // Accumulate the core requirements and track the maximum
      acc += c;
      maxCores = Math.max(maxCores, acc);
    }
//    System.out.println("maxCores="+maxCores);
    return maxCores;
  }
}
