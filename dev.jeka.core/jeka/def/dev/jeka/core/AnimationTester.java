package dev.jeka.core;

import dev.jeka.core.api.system.JkBusyIndicator;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.system.JkMemoryBufferLogDecorator;

public class AnimationTester {

    public static void main(String[] args) throws InterruptedException {
        JkLog.setDecorator(JkLog.Style.INDENT);
        JkLog.info("hello");
        JkLog.info("xooo");
        JkBusyIndicator.start("totopppppppppppppppppppppppppppppppppppppppppppppp");
        JkMemoryBufferLogDecorator.activateOnJkLog();
        System.out.println("------");
        //Thread.sleep(5000);
        JkBusyIndicator.stop();
        JkMemoryBufferLogDecorator.flush();
        JkMemoryBufferLogDecorator.inactivateOnJkLog();
        JkLog.info("finish");
    }
}
