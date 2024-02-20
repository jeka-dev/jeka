package _dev;

import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.JkInit;
import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.JkInjectClasspath;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.core.tool.builtins.tooling.ide.IntellijKBean;


@JkInjectClasspath("dev.jeka:nodejs-plugin:0.11.x-SNAPSHOT")
@JkInjectClasspath("org.junit.jupiter:junit-jupiter:5.10.2")
class Build extends KBean {

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
        // System.setOut(JkUtilsIO.nopPrintStream());
        System.err.println("hello");
    }

    public static void main(String[] args) {
        JkInit.kbean(Build.class, args).hello();
    }

}