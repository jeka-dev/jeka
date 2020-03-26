package dev.jeka.core.api.function;

import java.io.Serializable;
import java.util.function.UnaryOperator;

/**
 * A serializable {@link UnaryOperator}
 */
public interface JkUnaryOperator<T> extends UnaryOperator<T>, Serializable {
}
