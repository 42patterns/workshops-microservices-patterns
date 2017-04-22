package com.example.external.profanity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

@RunWith(Parameterized.class)
public class WordListTest {

    WordList wordList = new WordList(Arrays.asList("shi", "shit", "fuck", "ass"));

    @Parameterized.Parameters
    public static Collection<Object[]> data() {
        return Arrays.asList(new Object[][] {
                { "ok", false, "ok" },
                { "This is fine", false, "This is fine"},
                { "shit", true, "****"},
                { "shitty", true, "****ty" },
                { "What a shitty shit", true, "What a ****ty ****" }
        });
    }

    private final String input;
    private final boolean result;
    private final String escapedOutput;

    public WordListTest(String input, boolean result, String escapedOutput) {
        this.input = input;
        this.result = result;
        this.escapedOutput = escapedOutput;
    }

    @Test
    public void should_not_match_words() {
        assertThat(wordList.matches(input), is(result));
    }

    @Test
    public void should_escape_profane_words() {
        assertThat(wordList.escape(input), equalTo(escapedOutput));
    }

}