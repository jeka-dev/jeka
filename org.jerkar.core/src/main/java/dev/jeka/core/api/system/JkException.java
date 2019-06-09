package dev.jeka.core.api.system;

/**
 * Marker exception to notice a misconfiguration from the user.
 *
 * @author Jerome Angibaud
 */
public final class JkException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a {@link JkException} with the specified message.
     */
    public JkException(String message) {
        super(message);
    }

    /**
     * Throws a JkException with the specified message if the specified condition is met.
     */
    public static void throwIf(boolean condition, String message) {
        if (condition) {
            throw new JkException(message);
        }
    }

}
