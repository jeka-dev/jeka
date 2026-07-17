package com.example.lib;

import com.google.common.base.Strings;

public class Library {

    public String greet(String name) {
        return "Hello, " + Strings.nullToEmpty(name) + "!";
    }
}
