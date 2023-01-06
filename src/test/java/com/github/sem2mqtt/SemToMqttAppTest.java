package com.github.sem2mqtt;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

class SemToMqttAppTest {

  @Test()
  void fails_without_configuration_file() {
    assertThatCode(() -> SemToMqttApp.main(new String[0]))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("configuration");
  }

  @Test()
  void runs_when_configuration_exists() {
    String[] args = new String[1];
    args[0] = "valid_test.yaml";
    assertThatCode(() -> SemToMqttApp.main(args))
        .doesNotThrowAnyException();
  }

}