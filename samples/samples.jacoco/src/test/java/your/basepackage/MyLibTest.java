package your.basepackage;

import static org.junit.jupiter.api.Assertions.*;

class MyLibTest {

    @org.junit.jupiter.api.Test
    void toJsonArray() {
        assertEquals("[\"foo\", \"bar\"]", new MyLib().toJsonArray("foo", "bar"));
    }
}