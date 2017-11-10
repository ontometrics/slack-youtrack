package com.ontometrics.integrations.configuration;

import com.google.common.collect.ImmutableList;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;

public class ConfigurationTest {
    @Test
    public void testThatExcludedYoutrackPropertiesAreRead() {
        String[] excludedFields = ConfigurationFactory.get().getStringArray("excluded-youtrack-fields");
        List<String> excludedList = ImmutableList.copyOf(excludedFields);
        assertThat(excludedList, contains("Estimate Time", "Spent"));
    }
}
