package dev.jeka.core.api.depmanagement;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Tag to let IDE recognise that the value can be suggested with dependency coordinate
 */
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface JkDepSuggest {
}
