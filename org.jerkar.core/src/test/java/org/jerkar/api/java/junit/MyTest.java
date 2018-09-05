package org.jerkar.api.java.junit;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(JkUnitRunner.MyRunner.class)
public class MyTest {

    @Test
    public void myTestMethod() {
        System.out.println("----------------------------- my test method executed");
    }

}
