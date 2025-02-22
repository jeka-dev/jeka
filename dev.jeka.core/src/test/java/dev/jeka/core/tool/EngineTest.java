/*
 * Copyright 2014-2024  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package dev.jeka.core.tool;

import dev.jeka.core.api.project.JkCompileLayout;
import dev.jeka.core.api.project.JkProject;
import dev.jeka.core.api.system.JkProperties;
import dev.jeka.core.api.utils.JkUtilsIterable;
import dev.jeka.core.tool.builtins.project.ProjectKBean;
import org.junit.Test;

import static org.junit.Assert.*;

public class EngineTest {

    @Test
    public void defaultKBeanAsName_ok() {
        JkProject project = new EngineWrapper().run("project:", "test.skip=true", "layout.style=SIMPLE")
                .load(ProjectKBean.class).project;
        assertTrue(project.testing.isSkipped());
        assertEquals("src", project.compilation.layout.getSources().toList().get(0).getRoot().toString());
    }

    @Test
    public void injectFromJkProps_ok() {
        JkProperties props = JkProperties.ofMap(JkUtilsIterable.mapOf("@project.layout.style", "SIMPLE"));
        ProjectKBean projectKBean = new EngineWrapper().run(props,"project:")
                .load(ProjectKBean.class);
        assertEquals(JkCompileLayout.Style.SIMPLE, projectKBean.layout.style);
    }

    @Test
    public void injectFromCmdLineAndJkProps_cmdLineTakesPrecedence() {
        JkProperties props = JkProperties.ofMap(JkUtilsIterable.mapOf("@project.layout.style", "SIMPLE"));
        ProjectKBean projectKBean = new EngineWrapper().run(props,"project:", "layout.style=MAVEN")
                .load(ProjectKBean.class);
        assertEquals(JkCompileLayout.Style.MAVEN, projectKBean.layout.style);
    }

    @Test
    public void publicNestedFields_ok() {
        NestedProp nestedProp = new EngineWrapper(NestedProp.class).run("options.foo=a")
                .find(NestedProp.class).get();
        assertEquals("a", nestedProp.options.foo);
    }

    //@Test  // todo
    public void injectedProperty_ok() {
        NestedProp nestedProp = new EngineWrapper(NestedProp.class).run()
                .find(NestedProp.class).get();
        System.out.println(nestedProp.options.path);
        assertNotNull(nestedProp.options.path);
        assertNotEquals("null", nestedProp.options.path);
        assertFalse(nestedProp.options.path.startsWith("-"));
    }

    @Test
    public void picocliEnum_ok() {
        ProjectKBean projectKBean = new EngineWrapper()
                .run("project:", "scaffold.kind=REGULAR", "layout.style=SIMPLE")
                .load(ProjectKBean.class);
        assertEquals(JkCompileLayout.Style.SIMPLE, projectKBean.layout.style);
    }

    static class NestedProp extends KBean {

        public final Options options = new Options();

        @JkDoc
        public static class Options {
            public String foo = "";

            @JkPropValue("PATH")
            public String path;
        }

    }

}
