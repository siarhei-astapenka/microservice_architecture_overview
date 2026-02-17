package com.epam.learn.resource_processor.cucumber;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Cucumber runner for resource-processor component tests.
 * Configures Cucumber to scan for feature files and step definitions.
 */
@Suite
@SelectClasspathResource("features")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty")
public class ResourceProcessorCucumberRunner {
}
