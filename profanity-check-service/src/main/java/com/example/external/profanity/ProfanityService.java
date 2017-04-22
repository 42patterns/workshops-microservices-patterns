package com.example.external.profanity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ProfanityService {

    final static Logger log = LoggerFactory.getLogger(ProfanityService.class);
    final WordList wordList;

    public ProfanityService(WordList wordList) {
        this.wordList = wordList;
    }

    public IsSwearWord profanityCheck(String input) {
        boolean contains = wordList.matches(input);
        IsSwearWord isSwearWord;
        if (contains) {
            isSwearWord = new IsSwearWord(contains, input, wordList.escape(input));
        } else {
            isSwearWord = new IsSwearWord(contains, input);
        }

        log.info(isSwearWord.toString());
        return isSwearWord;
    }

}
