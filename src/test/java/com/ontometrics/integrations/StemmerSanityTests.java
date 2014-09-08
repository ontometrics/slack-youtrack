package com.ontometrics.integrations;

import org.junit.Test;
import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Created by Rob on 9/7/14.
 * Copyright (c) ontometrics, 2014 All Rights Reserved
 */
public class StemmerSanityTests {

    @Test
    public void englishSanityCheck() {

        SnowballStemmer snowballStemmer = new englishStemmer();
        snowballStemmer.setCurrent("Jumps");
        snowballStemmer.stem();
        String result = snowballStemmer.getCurrent();

        assertThat(result, is("Jump"));

    }
}
