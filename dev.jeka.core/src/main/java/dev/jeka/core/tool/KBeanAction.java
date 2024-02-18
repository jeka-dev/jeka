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

    // Return KBean action for a single scoped kbean subcommands
    CreateResult createFrom(CmdLineArgs args, EngineBase.KBeanResolution resolution) {

        // Find KBean class tto which apply the args
        String firstArg = args.get()[0];
        final String kbeanName;
        final  String kbeanClassName;
        final String[] methodOrFieldArgs;
        if (CmdLineArgs.isKbeanRef(firstArg)) {
            kbeanName = firstArg.substring(0, firstArg.length()-1);
            kbeanClassName = resolution.findKbeanClassName(kbeanName).orElse(null);
            methodOrFieldArgs = Arrays.copyOfRange(args.get(), 1, args.get().length);
        } else {
            kbeanName = "defaultKbean";
            kbeanClassName = resolution.defaultKbeanClassname;
            methodOrFieldArgs = args.get();
        }
        if (kbeanClassName == null) {
            return new CreateResult(kbeanName, null);
        }
        Class<? extends KBean> kbeanClass = JkClassLoader.ofCurrent().load(kbeanClassName);

        // Create a PicoCli commandLine to parse
        KBeanDescription kBeanDescription = KBeanDescription.of(kbeanClass, false);
        CommandLine commandLine = new CommandLine(PicocliCommands.fromKBeanDesc(kBeanDescription));
        CommandLine.ParseResult parseResult = commandLine.parseArgs(methodOrFieldArgs);

        List<KBeanAction> kBeanActions = new LinkedList<>();

        // Add init action
        kBeanActions.add(new KBeanAction(BEAN_INSTANTIATION, kbeanClassName, null, null));

        // Add field actions
        for (CommandLine.Model.OptionSpec optionSpec : parseResult.matchedOptions()) {
            String name = optionSpec.names()[0];
            Object value = parseResult.matchedOptionValue(name, null);
            KBeanAction kBeanAction = new KBeanAction(PROPERTY_INJECT, kbeanName, name, value);
            kBeanActions.add(kBeanAction);
        }





        return new CreateResult(null, kBeanActions);
    }

    static class CreateResult {

        final String unmatchedKBean;

        final List<KBeanAction> kBeanActions;

        CreateResult(String unmatchedKBean, List<KBeanAction> kBeanActions) {
            this.unmatchedKBean = unmatchedKBean;
            this.kBeanActions = kBeanActions;
        }
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
