package dev.jeka.core.tool;

import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;

/**
 * Represents an action to be taken on a bean, such as invoking a method,
 * injecting a property, or instantiating a bean.
 */
class KBeanAction {

    final EngineCommand.Action action;

    final String beanName;

    final String member; // method or property name

    final String value; // if property

    /*
     * Creates a JkActionBean by parsing its textual representation, such as 'someBean#someValue=bar'.
     */
    KBeanAction(String expression) {
        final String beanExpression;
        if (expression.contains(CmdLine.KBEAN_SYMBOL)) {
            String before = JkUtilsString.substringBeforeFirst(expression, CmdLine.KBEAN_SYMBOL);

            // Normally, if we want to refer to the default KBean, we can just mention '#someProperty'.
            // However, if we want to refer to a such property from a property file, this won't work, as
            // starting with a '#' will be interpreted as a comment.
            // So we let the possibility of doing so, by using the 'kb#' member prefix in place of '#'.
            if (Environment.KB_KEYWORD.equals(before)) {
                before = null;
            }
            this.beanName = JkUtilsString.isBlank(before) ? null : before;
            beanExpression = JkUtilsString.substringAfterFirst(expression, CmdLine.KBEAN_SYMBOL);
        } else {
            System.err.println("Usage of '" + expression + "' is deprecated. Use '#" + expression + "' instead.");
            this.beanName = null;
            beanExpression = expression;
        }
        if (beanExpression.isEmpty()) {
            this.action = EngineCommand.Action.BEAN_INSTANTIATION;
            this.member = null;
            this.value = null;
        } else if (beanExpression.contains("=")) {
            this.action = EngineCommand.Action.PROPERTY_INJECT;
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
        } else if (action == EngineCommand.Action.PROPERTY_INJECT) {
            actionName = "field";
        } else {
            actionName = "constructor";
        }
        return String.format("%s '%s'", actionName, member);
    }
}
