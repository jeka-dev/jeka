package dev.jeka.core.tool;

import java.util.List;

class PicocliEngineWrapper extends EngineWrapper {

    @Override
    protected List<KBeanAction> parse(String[] args, EngineBase.KBeanResolution kBeanResolution) {
        return PicocliParser.parse(new CmdLineArgs(args), kBeanResolution);
    }
}
