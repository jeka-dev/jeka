package _dev;

import dev.jeka.core.tool.KBean;
import dev.jeka.core.tool.JkDep;
import dev.jeka.core.tool.builtins.base.BaseKBean;
import dev.jeka.plugins.springboot.SpringbootKBean;

${inject}

@JkDep("${springboot-plugin}")

class Build extends KBean {

    @Override
    protected void init() {
        load(BaseKBean.class);
        load(SpringbootKBean.class);
    }

}
