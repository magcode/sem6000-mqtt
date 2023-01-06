package com.github.sem2mqtt.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.time.Duration;
import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class BridgeConfigurationLoader {

  public static final Logger LOGGER = LogManager.getLogger(BridgeConfigurationLoader.class);
  public static final String DEFAULT_PROPERTY_FILENAME = "sem6.properties";
  public static final String DEFAULT_YAML_FILENAME = "sem2mqtt_bridge.yaml";

  private final ObjectMapper yamlMapper;

  public BridgeConfigurationLoader(ObjectMapper yamlMapper) {
    this.yamlMapper = yamlMapper;
  }

  public BridgeConfiguration load(String configurationFileName) {
    File file = getClassPathFileFor(configurationFileName);
    failIfFileDoesNotExist(file);
    if (configurationFileName.endsWith(".yaml")) {
      return loadFromYaml(file);
    } else if (configurationFileName.endsWith(".properties")) {
      return loadFromProperties(file);
    }

    throw new IllegalArgumentException("Unknown file type.");
  }

  private void failIfFileDoesNotExist(File file) {
    if (!file.exists()) {
      throw new IllegalArgumentException(String.format(
          "Configuration file '%s' does not exist.", file.getName()));
    }
  }

  public BridgeConfiguration load() {
    LOGGER.debug("Scanning for configuration file.");
    File propertiesFile = getClassPathFileFor("./" + DEFAULT_PROPERTY_FILENAME);
    if (propertiesFile.exists()) {
      LOGGER.debug("Found '" + DEFAULT_PROPERTY_FILENAME + "'.");
      return loadFromProperties(propertiesFile);
    }

    File yamlFile = getClassPathFileFor("./" + DEFAULT_YAML_FILENAME);
    if (yamlFile.exists()) {
      LOGGER.debug("Found '" + DEFAULT_YAML_FILENAME + "'.");
      return loadFromYaml(yamlFile);
    }

    LOGGER.error("No configuration file found.");
    throw new IllegalArgumentException(
        "You need to offer configuration in either '" + DEFAULT_PROPERTY_FILENAME + "' or '" + DEFAULT_YAML_FILENAME
            + "'.");
  }

  private BridgeConfiguration loadFromProperties(File propertiesFile) {
    LOGGER.info(String.format("Loading config from '%s'", propertiesFile.getName()));
    try {
      Properties props = new Properties();
      props.load(Files.newInputStream(propertiesFile.toPath()));
      MqttConfig mqttConfig = new MqttConfig(props.getProperty("rootTopic"),
          props.getProperty("mqttServer"), props.getProperty("mqttClientId"),
          props.getProperty("mqttUsername"), props.getProperty("mqttPassword"));
      Set<Sem6000Config> semConfigs = new HashSet<>();
      for (int i = 1; i < 11; i++) {
        if (props.containsKey("sem" + i + ".mac")) {
          semConfigs.add(new Sem6000Config(props.getProperty("sem" + i + ".mac"),
              props.getProperty("sem" + i + ".pin"),
              props.getProperty("sem" + i + ".name"),
              Optional.ofNullable(props.getProperty("sem" + i + ".refresh"))
                  .map(Integer::valueOf).map(
                      Duration::ofSeconds).orElse(null)));
        }
      }

      LOGGER.info("Successfully loaded properties config.");
      return new BridgeConfiguration(mqttConfig, semConfigs);
    } catch (IOException e) {
      failOnReadError(e, propertiesFile.getName());
      throw new RuntimeException(e);
    }
  }

  private BridgeConfiguration loadFromYaml(File yamlFile) {
    LOGGER.info(String.format("Loading config from '%s'", yamlFile.getName()));
    try {
      BridgeConfiguration bridgeConfiguration = yamlMapper.readValue(yamlFile, BridgeConfiguration.class);
      LOGGER.info("Successfully loaded yaml config.");
      return bridgeConfiguration;
    } catch (IOException e) {
      failOnReadError(e, yamlFile.getName());
      throw new RuntimeException(e);
    }
  }

  private void failOnReadError(Exception e, String fileName) {
    LOGGER.error(String.format("Failed to load configuration file '%s'.", fileName), e);
  }

  private File getClassPathFileFor(String filePath) {
    URL resource = this.getClass().getClassLoader().getResource(filePath);
    return Optional.ofNullable(resource).map(URL::getFile).map(File::new).orElse(new File(""));
  }
}
