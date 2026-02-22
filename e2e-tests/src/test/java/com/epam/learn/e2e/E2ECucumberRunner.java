package com.epam.learn.e2e;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;

/**
 * JUnit Platform Suite runner for the End-to-End Cucumber test suite.
 *
 * <p>Discovers all {@code .feature} files under {@code classpath:features/}
 * and wires them to the step definitions in {@link E2EStepDefinitions} and
 * the infrastructure hooks in {@link E2ECucumberHooks}.
 *
 * <p>To run only a subset of scenarios, override the tag filter via the
 * Maven Surefire property:
 * <pre>
 *   mvn test -Dcucumber.filter.tags="@e2e and @happy-path"
 * </pre>
 *
 * <p>Reports are written to {@code target/cucumber-reports/}.
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME,         value = "com.epam.learn.e2e")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME,       value = "pretty, html:target/cucumber-reports/e2e-report.html, json:target/cucumber-reports/e2e-report.json")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME,  value = "@e2e")
public class E2ECucumberRunner {
    // JUnit Platform Suite – no body required.
}
