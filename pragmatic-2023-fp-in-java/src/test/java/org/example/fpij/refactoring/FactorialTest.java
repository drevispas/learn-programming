package org.example.fpij.refactoring;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class FactorialTest {

  Factorial factorial;

  @BeforeEach
  public void init() {
    factorial = new Factorial();
  }

  @Test
  public void computeFactorial() {
    assertAll(
        () -> assertEquals(BigInteger.ONE, factorial.compute(1)),
        () -> assertEquals(BigInteger.TWO, factorial.compute(2)),
        () -> assertEquals(BigInteger.valueOf(6), factorial.compute(3)),
        () -> assertEquals(BigInteger.valueOf(120), factorial.compute(5))
    );
  }
}
