package dev.jeka.core.api.text;

import org.junit.Test;

public class JkColumnTextTest {

    @Test
    public void test_ok()  {
        JkColumnText columnText = JkColumnText
                .ofSingle(20, 30)
                .addColumn(10, 40)
                .addColumn(20, 50);
        columnText
                .add("aaaaaaaaa","bbbbbbbbbb bbbbbb", "cccccccc ccccccc")
                .add("cc", "d", "eeeeeeeeeeee eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")
                .add("fffffffff F1F1F1F11F", "gggg g  gggggg", "hhhhhhhh HHHHH" );
        System.out.println(columnText);
    }

}