package dev.jeka.core.tool;

/**
 * Marker exception to notice a misconfiguration from the user.
 *
 * @author Jerome Angibaud
 */
final class JkException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a {@link JkException} with the specified message.
     */
    public JkException(String message, Object ... args) {
        super(String.format(message, args));
    }

    public JkException(Exception cause, String message, Object ...args) {
        super(String.format(message, args), cause);
    }

}
