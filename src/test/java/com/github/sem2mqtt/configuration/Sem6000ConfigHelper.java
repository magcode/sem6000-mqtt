package com.github.sem2mqtt.configuration;

import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

import java.time.Duration;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class Sem6000ConfigHelper {

  public static Set<Sem6000Config> generateSemConfigs(int countOfSems) {
    return
        IntStream.range(0, countOfSems)
            .mapToObj(num -> randomSemConfigForPlug("plug" + num))
            .collect(toSet());
  }

  public static Sem6000Config randomSemConfigForPlug(String plugName) {
    return new Sem6000Config(randomMac(), randomPin(), plugName, Duration.ofSeconds(60));
  }

  static String randomPin() {
    return String.valueOf(ThreadLocalRandom.current().nextInt(100000));
  }

  static String randomMac() {
    return ThreadLocalRandom.current().ints()
        .limit(6)
        .map(num -> num % 100)
        .map(Math::abs)
        .mapToObj(String::valueOf)
        .collect(joining(":"));
  }
}
