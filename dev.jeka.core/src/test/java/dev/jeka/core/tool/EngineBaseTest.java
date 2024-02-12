package dev.jeka.core.tool;

import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class EngineBaseTest {

    @Test
    public void projectHelp_ok() {
        ///JkLog.setDecorator(JkLog.Style.FLAT);
        EngineWrapper.of().run("project#help").cleanDir();
    }

    @Test
    public void defaultKBeanAsName_ok() {
        JkProject project = EngineWrapper.of().run("-kb=project", "#tests.skip=true", "#layout.style=SIMPLE")
                .load(ProjectKBean.class).project;
        assertTrue(project.testing.isSkipped());
        assertEquals("src", project.compilation.layout.getSources().toList().get(0).getRoot().toString());
    }

    @Test
    public void publicNestedProp_ok() {
        NestedProp nestedProp = EngineWrapper.of(NestedProp.class).run("#options.foo=a")
                .find(NestedProp.class).get();
        Assert.assertEquals("a", nestedProp.options.foo);
    }



    static class NestedProp extends KBean {

        public Options options = new Options();

        static class Options {
            public String foo = "";
        }

    }


}
