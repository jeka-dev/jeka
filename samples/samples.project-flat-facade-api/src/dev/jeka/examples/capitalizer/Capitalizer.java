package dev.jeka.examples.capitalizer;

import com.google.common.base.Splitter;

public class Capitalizer {

    public static String capitalize(String sentence) {
        Splitter splitter = Splitter.on(" ");
        StringBuilder result = new StringBuilder();
        for (String item : splitter.split(sentence)) {
            result.append(capitalizeWord(item)).append(" ");
        }
        result.deleteCharAt(result.length() - 1);
        return result.toString();
    }

    private static String capitalizeWord(String word) {
        char first = word.charAt(0);
        String firstLetter = Character.toString(first);
        return firstLetter.toUpperCase() + word.substring(1);
    }

}
