package dev.jeka.core.tool;

import java.util.UnknownFormatConversionException;

/**
 * Marker exception to notice a misconfiguration from the user.
 * When such an exception is raised, Jeka does not display stacktrace on output console.
 *
 * @author Jerome Angibaud
 */
final class JkException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a {@link JkException} with the specified message.
     */
    public JkException(String message, Object ... args) {
        super(message(message, args));
    }

    private static String message(String message, Object ... args) {
        try {
            return String.format(message, args);
        } catch (UnknownFormatConversionException e) {
            return message;
        }
    }

    public JkException(Exception cause, String message, Object ...args) {
        super(String.format(message, args), cause);
    }

}
