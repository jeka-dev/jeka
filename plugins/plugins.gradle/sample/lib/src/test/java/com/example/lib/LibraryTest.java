package com.example.lib;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LibraryTest {

    @Test
    void greet_returnsExpectedMessage() {
        assertEquals("Hello, World!", new Library().greet("World"));
    }

    @Test
    void greet_withNull_returnsEmpty() {
        assertEquals("Hello, !", new Library().greet(null));
    }
}
