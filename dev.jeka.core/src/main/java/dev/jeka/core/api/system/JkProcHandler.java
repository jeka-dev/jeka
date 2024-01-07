package dev.jeka.core.api.system;

/**
 * Represents a handler for a running process.
 * This class provides methods to interact with the process.
 */
public class JkProcHandler {

    private final Process process;


    JkProcHandler(Process process) {
        this.process = process;
    }

    /**
     * Returns the process associated with this handler.
     */
    public Process getProcess() {
        return process;
    }

}
