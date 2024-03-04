package dev.jeka.core.tool.builtins.admin;

import dev.jeka.core.api.system.JkLocator;
import dev.jeka.core.api.system.JkProcess;
import dev.jeka.core.api.utils.JkUtilsSystem;
import dev.jeka.core.tool.JkDoc;
import dev.jeka.core.tool.KBean;

import java.awt.*;
import java.io.IOException;
import java.nio.file.Path;

@JkDoc("Provides convenient methods to perform admin tasks")
public class AdminKBean extends KBean {

    @JkDoc("Open a file explorer window on JeKA user home dir.")
    public void openHomeDir() throws IOException {
        Desktop.getDesktop().open(JkLocator.getJekaUserHomeDir().toFile());
    }

    @JkDoc("Edit global.properties file.")
    public void editGlobalConf() throws IOException {
        Path globalProps = JkLocator.getGlobalPropertiesFile();
        if (!GraphicsEnvironment.isHeadless()) {
            Desktop.getDesktop().edit(globalProps.toFile());
        } else if (!JkUtilsSystem.IS_WINDOWS) {
            JkProcess.of("nano", globalProps.toString())
                    .setInheritIO(true)
                    .exec();
        }

    }


}
