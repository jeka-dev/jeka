package _dev;

import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.builtins.self.SelfKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;


@JkInjectClasspath("dev.jeka:nodejs-plugin:0.11.x-SNAPSHOT")
@JkInjectClasspath("org.junit.jupiter:junit-jupiter:5.10.2")
class Build extends KBean {

    @Override
    protected void init() {
        load(IntellijKBean.class)
                .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core");
        load(SelfKBean.class)
                .setMainClass(null);
    }

}