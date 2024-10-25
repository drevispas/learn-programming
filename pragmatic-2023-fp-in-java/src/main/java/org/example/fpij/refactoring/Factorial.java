package org.example.fpij.refactoring;

import java.math.BigInteger;
import java.util.stream.LongStream;

public class Factorial {

  public BigInteger compute(long upTo) {
    return LongStream.rangeClosed(1,upTo)
        .mapToObj(BigInteger::valueOf)
        .reduce(BigInteger.ONE, BigInteger::multiply);
  }
}
