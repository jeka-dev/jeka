package dev.jeka.core.tool;

import dev.jeka.core.api.java.JkClassLoader;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static dev.jeka.core.tool.EngineCommand.Action.BEAN_INSTANTIATION;
import static dev.jeka.core.tool.EngineCommand.Action.PROPERTY_INJECT;

/**
 * Represents an action to be taken on a bean, such as invoking a method,
 * injecting a property, or instantiating a bean.
 */
class KBeanAction {

    final EngineCommand.Action action;

    final String beanName;

    final String member; // method or property name

    final Object value; // if property

    public KBeanAction(EngineCommand.Action action, String beanName, String member, Object value) {
        this.action = action;
        this.beanName = beanName;
        this.member = member;
        this.value = value;
    }

    /*
     * Creates a JkActionBean by parsing its textual representation, such as 'someBean#someValue=bar'.
     */
    KBeanAction(String expression) {
        final String beanExpression;
        if (expression.contains(ParsedCmdLine.KBEAN_SYMBOL)) {
            String before = JkUtilsString.substringBeforeFirst(expression, ParsedCmdLine.KBEAN_SYMBOL);

            // Normally, if we want to refer to the default KBean, we can just mention '#someProperty'.
            // However, if we want to refer to a such property from a property file, this won't work, as
            // starting with a '#' will be interpreted as a comment.
            // So we let the possibility of doing so, by using the 'kb#' member prefix in place of '#'.
            if (Environment.KB_KEYWORD.equals(before)) {
                before = null;
            }
            this.beanName = JkUtilsString.isBlank(before) ? null : before;
            beanExpression = JkUtilsString.substringAfterFirst(expression, ParsedCmdLine.KBEAN_SYMBOL);
        } else {
            this.beanName = null;
            beanExpression = expression;
        }
        if (beanExpression.isEmpty()) {
            this.action = BEAN_INSTANTIATION;
            this.member = null;
            this.value = null;
        } else if (beanExpression.contains("=")) {
            this.action = PROPERTY_INJECT;
            this.member = JkUtilsString.substringBeforeFirst(beanExpression, "=");
            JkUtilsAssert.argument(!this.member.isEmpty(), "Illegal expression " + expression);
            this.value = JkUtilsString.substringAfterFirst(beanExpression, "=");
        } else {
            this.action = EngineCommand.Action.METHOD_INVOKE;
            this.member = beanExpression;
            this.value = null;
        }
    }

    @Override
    public String toString() {
        return "action=" + action + ", beanName='" + beanName + '\'' + ", member='" + member + '\'' +
                ", value='" + value + '\'';
    }



    String shortDescription() {
        String actionName = null;
        if (action == EngineCommand.Action.METHOD_INVOKE) {
            actionName = "method";
        } else if (action == PROPERTY_INJECT) {
            actionName = "field";
        } else {
            actionName = "constructor";
        }
        return String.format("%s '%s'", actionName, member);
    }

}
