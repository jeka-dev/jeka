package dev.jeka.core.tool;

import java.util.List;

class EngineCommands {

    private List<EngineCommand> engineCommands;

    EngineCommands(List<EngineCommand> engineCommands) {
        this.engineCommands = engineCommands;
    }

    List<EngineCommand> commands() {
        return engineCommands;
    }

}
