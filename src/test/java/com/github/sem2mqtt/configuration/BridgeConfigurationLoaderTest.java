package com.github.sem2mqtt.configuration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import java.time.Duration;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BridgeConfigurationLoaderTest {

  private BridgeConfigurationLoader loader;

  @BeforeEach
  void setUp() {
    ObjectMapper mapper = new ObjectMapper(new YAMLFactory());
    mapper.findAndRegisterModules();
    loader = new BridgeConfigurationLoader(mapper);
  }

  @Test
  void mqtt_config_matches_yaml_content() {
    //when
    MqttConfig mqttConfig = loader.load("valid_test.yaml").getMqttConfig();
    //then
    assertThat(mqttConfig.getRootTopic()).isEqualTo("home/mysemdevices");
    assertThat(mqttConfig.getUrl()).isEqualTo("tcp://192.168.0.1");
    assertThat(mqttConfig.getClientId()).isEqualTo("client-for-sem6000");
    assertThat(mqttConfig.getUsername()).isEqualTo("mqttUsername");
    assertThat(mqttConfig.getPassword()).isEqualTo("mqttSecret");
  }

  @Test
  void contains_both_sem_configs_matching_yaml_content() {
    //when
    Set<Sem6000Config> semConfigs = loader.load("valid_test.yaml").getSemConfigs();
    //then
    assertThat(semConfigs).hasSize(2);
    assertThat(semConfigs).anySatisfy(sem6000Config -> {
      assertThat(sem6000Config.getMac()).isEqualTo("00:00:00:00:00:01");
      assertThat(sem6000Config.getPin()).isEqualTo("0000");
      assertThat(sem6000Config.getName()).isEqualTo("sem1");
      assertThat(sem6000Config.getUpdateInterval()).isEqualTo(Duration.ofSeconds(10));
    });
    assertThat(semConfigs).anySatisfy(sem6000Config -> {
      assertThat(sem6000Config.getMac()).isEqualTo("00:00:00:00:00:02");
      assertThat(sem6000Config.getPin()).isEqualTo("1234");
      assertThat(sem6000Config.getName()).isEqualTo("sem 2");
      assertThat(sem6000Config.getUpdateInterval()).isEqualTo(Duration.ofMinutes(15));
    });
  }

  @Test
  void sets_defaults_when_yaml_keys_are_missing() {
    //when
    BridgeConfiguration bridgeConfiguration = loader.load("minimal.yaml");
    MqttConfig mqttConfig = bridgeConfiguration.getMqttConfig();
    Set<Sem6000Config> semConfigs = bridgeConfiguration.getSemConfigs();
    //then
    assertThat(mqttConfig.getRootTopic()).isEqualTo("home");
    assertThat(mqttConfig.getUrl()).isEqualTo("tcp://localhost");
    assertThat(mqttConfig.getClientId()).isEqualTo("semtomqttbridge");
    assertThat(mqttConfig.getUsername()).isNull();
    assertThat(mqttConfig.getPassword()).isNull();

    assertThat(semConfigs).hasSize(1);
    assertThat(semConfigs).anySatisfy(sem6000Config -> {
      assertThat(sem6000Config.getPin()).isEqualTo("0000");
      assertThat(sem6000Config.getUpdateInterval()).isEqualTo(Duration.ofSeconds(60));
    });
  }

  @Test
  void mqtt_config_matches_properties_content() {
    //when
    MqttConfig mqttConfig = loader.load("valid_test.properties").getMqttConfig();
    //then
    assertThat(mqttConfig.getRootTopic()).isEqualTo("home/mysemdevices");
    assertThat(mqttConfig.getUrl()).isEqualTo("tcp://192.168.0.1");
    assertThat(mqttConfig.getClientId()).isEqualTo("client-for-sem6000");
    assertThat(mqttConfig.getUsername()).isEqualTo("mqttUsername");
    assertThat(mqttConfig.getPassword()).isEqualTo("mqttSecret");
  }

  @Test
  void contains_both_sem_configs_matching_properties_content() {
    //when
    Set<Sem6000Config> semConfigs = loader.load("valid_test.properties").getSemConfigs();
    //then
    assertThat(semConfigs).anySatisfy(sem6000Config -> {
      assertThat(sem6000Config.getMac()).isEqualTo("00:00:00:00:00:01");
      assertThat(sem6000Config.getPin()).isEqualTo("0000");
      assertThat(sem6000Config.getName()).isEqualTo("sem1");
      assertThat(sem6000Config.getUpdateInterval()).isEqualTo(Duration.ofSeconds(60));
    });
    assertThat(semConfigs).anySatisfy(sem6000Config -> {
      assertThat(sem6000Config.getMac()).isEqualTo("00:00:00:00:00:02");
      assertThat(sem6000Config.getPin()).isEqualTo("1234");
      assertThat(sem6000Config.getName()).isEqualTo("sem 2");
      assertThat(sem6000Config.getUpdateInterval()).isEqualTo(Duration.ofMinutes(15));
    });
  }

  @Test
  void sets_defaults_when_properties_are_missing() {
    //when
    BridgeConfiguration bridgeConfiguration = loader.load("minimal.properties");
    MqttConfig mqttConfig = bridgeConfiguration.getMqttConfig();
    Set<Sem6000Config> semConfigs = bridgeConfiguration.getSemConfigs();
    //then
    assertThat(mqttConfig.getRootTopic()).isEqualTo("home");
    assertThat(mqttConfig.getUrl()).isEqualTo("tcp://localhost");
    assertThat(mqttConfig.getClientId()).isEqualTo("semtomqttbridge");
    assertThat(mqttConfig.getUsername()).isNull();
    assertThat(mqttConfig.getPassword()).isNull();

    assertThat(semConfigs).hasSize(1);
    assertThat(semConfigs).anySatisfy(sem6000Config -> {
      assertThat(sem6000Config.getPin()).isEqualTo("0000");
      assertThat(sem6000Config.getUpdateInterval()).isEqualTo(Duration.ofSeconds(60));
    });
  }
}