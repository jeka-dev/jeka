package com.example.app;

import com.example.lib.Library;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class App {

    private static final Logger logger = LoggerFactory.getLogger(App.class);

    public static void main(String[] args) {
        Library library = new Library();
        String message = library.greet("World");
        logger.info(message);
    }
}
