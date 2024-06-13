package dev.jeka.core.api.text;

import org.junit.Test;

public class JkColumnTextTest {

    @Test
    public void test_ok()  {
        JkColumnText columnText = JkColumnText
                .ofSingle(20, 30)
                .addColumn(10, 40)
                .addColumn(1, 150);
        columnText
                .add("aaaaaaaaa","bbbbbbbbbb bbbbbb", "cccccccc ccccccc")
                .add("cc", "d", "eeeeeeeeeeee eeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeeee")
                .add("fffffffffhhhhh F1F1F1F11F", "gggg g  gggggg", "hhhhhhhh HHHHH" );
        System.out.print(columnText);
        System.out.println("---------");
    }

    @Test
    public void test2_ok()  {
        JkColumnText columnText = JkColumnText
                .ofSingle(2, 30)
                .addColumn(1, 40)
                .addColumn(5, 60)
                .setMarginLeft("    ")
                .setSeparator(" | ");
        columnText
                .add("aaaaaaaaa","bbbbbbbbbb bbbbbb", "11111")
                .add("cc", "dijjjjjjjjjjyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyyjjj", "3333333")
                .add("fffffffff F1F1F1F11F", "gggg g  gggggg", "11111" );
        System.out.print(columnText);
        System.out.println("---------");
    }



}