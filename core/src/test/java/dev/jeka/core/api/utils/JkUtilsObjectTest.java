package dev.jeka.core.api.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JkUtilsObjectTest {

    private static class Foo {

        public Integer aField;
    }

    private static class Bar {

        public String aString;

        public Foo aFoo = new Foo();
    }

    @Test
    public void mergePublicFieldsInto() {
        Bar originalBar = new Bar();
        originalBar.aString = "coco";
        originalBar.aFoo = new Foo();
        originalBar.aFoo.aField = 1;

        Bar overriderBar = new Bar();
        overriderBar.aString = "tutu";
        overriderBar.aFoo = new Foo();
        overriderBar.aFoo.aField = 3;

        JkUtilsObject.copyNonNullPublicFieldsInto(originalBar, overriderBar);
        assertEquals(overriderBar.aString, originalBar.aString);
        assertEquals(overriderBar.aFoo.aField, originalBar.aFoo.aField);

        // -- null on original
        originalBar = new Bar();
        originalBar.aString = null;
        originalBar.aFoo = null;

        overriderBar = new Bar();
        overriderBar.aString = "tutu";
        overriderBar.aFoo = new Foo();
        overriderBar.aFoo.aField = 3;

        JkUtilsObject.copyNonNullPublicFieldsInto(originalBar, overriderBar);
        assertEquals(overriderBar.aString, originalBar.aString);
        assertEquals(overriderBar.aFoo.aField, originalBar.aFoo.aField);

        // -- null on overrider
        originalBar = new Bar();
        originalBar.aString = "coco";
        originalBar.aFoo = new Foo();
        originalBar.aFoo.aField = 1;

        overriderBar = new Bar();
        overriderBar.aString = null;
        overriderBar.aFoo = new Foo();
        overriderBar.aFoo.aField = null;

        JkUtilsObject.copyNonNullPublicFieldsInto(originalBar, overriderBar);
        assertEquals("coco", originalBar.aString);
        assertEquals(1, (int) originalBar.aFoo.aField);
    }
}