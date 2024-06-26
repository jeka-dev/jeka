package dev.jeka.core.api.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JkUtilsObjectTest {

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
        assertTrue(1 == originalBar.aFoo.aField);

    }
}