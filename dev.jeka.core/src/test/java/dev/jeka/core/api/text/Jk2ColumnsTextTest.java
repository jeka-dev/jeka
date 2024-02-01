package dev.jeka.core.api.text;

import org.junit.Test;



public class Jk2ColumnsTextTest {

    @Test
    public void format() {
       String text = Jk2ColumnsText.of(8, 30).setMarginLeft("  ")
               .add("title 1 hjhjhjhjhjh", "description 1")
               .add("title 2", "description 2")
               .add("title 3", "blabla blabla blabla blabla blabla blabla blabla blablablabla")
               .add("title 4", "description 4")
               .add("title 5 realy to long", "description 5")
               .add("title 6", "a desc \nwith breaking lines")
               .add("title 7", "description 7")
               .toString();
        System.out.println("------");
        System.out.println(text);
    }
}