package _dev;

import dev.jeka.core.tool.*;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;

@JkInjectClasspath("dev.jeka:nodejs-plugin:0.11.x-SNAPSHOT")
@JkInjectClasspath("org.junit.jupiter:junit-jupiter:5.10.2")
class Build extends KBean {

    @JkInjectProperty("PATH")
    public String path;

    @Override
    protected void init() {
        load(IntellijKBean.class)
                .replaceLibByModule("dev.jeka.jeka-core.jar", "dev.jeka.core");
        load(BaseKBean.class)
                .setMainClass(null);
    }

    @JkDoc("Used by sample tests o check if this bean is considered as the default kbean")
    public void ok() {
        System.out.println("ok");
    }

    public void hello() {
        System.err.println("hello");
    }

    public void printPath() {
        System.err.println(path);
    }

    public static void main(String[] args) {
        JkInit.kbean(Build.class, args).hello();
    }

}