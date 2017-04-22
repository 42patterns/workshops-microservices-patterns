package com.example.external.profanity;

import org.hamcrest.CoreMatchers;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.*;

public class WordListIntegrationTest {

    final WordList wordList;

    public WordListIntegrationTest() throws IOException, URISyntaxException {
        this.wordList = new WordList();
    }

    @Test
    public void should_escape_input() {
        assertThat(wordList.escape("This is shittiest shit"), equalTo("This is ****tiest ****"));
    }
}