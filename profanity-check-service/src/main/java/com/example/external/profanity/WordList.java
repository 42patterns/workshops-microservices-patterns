package com.example.external.profanity;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Comparator.comparing;

@Component
class WordList {

    final private List<String> obsceneWords;

    public WordList() throws URISyntaxException, IOException {
        InputStream is = WordList.class.getResourceAsStream("/words.txt");
        obsceneWords = new BufferedReader(new InputStreamReader(is)).lines()
                .collect(Collectors.toList());
    }

    WordList(List<String> defaultList) {
        obsceneWords = defaultList;
    }

    public boolean matches(String input) {
        return (obsceneWords.stream()
                .filter(s -> input.contains(s)).count() > 0);
    }

    public String escape(String input) {
        Function<String, Optional<String>> replace = word -> obsceneWords
                .stream()
                .sorted(comparing(String::length).reversed())
                .filter(word::contains)
                .map(s -> word.replaceAll(s, replaceWithStars.apply(s)))
                .findFirst();

        String collect = Arrays.stream(input.split(" "))
                .map(s -> new AbstractMap.SimpleEntry<>(s, replace.apply(s)))
                .flatMap(kv -> kv.getValue().map(v -> Stream.of(v)).orElse(Stream.of(kv.getKey())))
                .collect(Collectors.joining(" "));

        return collect;
    }

    public static Function<String, String> replaceWithStars = s -> s.chars()
            .map(c ->Character.valueOf('*'))
            .collect(() -> new StringBuilder(),
                (sb, i) -> sb.append((char) i),
                (sb1, sb2) -> sb1.append(sb2)
            ).toString();
}
