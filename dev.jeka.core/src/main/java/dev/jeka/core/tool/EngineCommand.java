package dev.jeka.core.tool;

import dev.jeka.core.api.text.JkColumnText;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;

import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

class EngineCommand {

    enum Action {

        PROPERTY_INJECT("set-value"), METHOD_INVOKE("invoke"), BEAN_INSTANTIATION("initialize");

        private final String name;

        Action(String name) {
            this.name = name;
        }
    }

    private final Action action;

    private final Class<? extends KBean> beanClass;

    private final String member;

    private final Object value;  // for properties only

    EngineCommand(Action action, Class<? extends KBean> beanClass, String valueOrMethod, Object value) {
        JkUtilsAssert.argument(beanClass != null, "KBean class cannot be null.");
        this.action = action;
        this.beanClass = beanClass;
        this.member = valueOrMethod;
        this.value = value;
    }

    public Action getAction() {
        return action;
    }

    public Class<? extends KBean> getBeanClass() {
        return beanClass;
    }

    public String getMember() {
        return member;
    }

    public Object getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "EngineCommand{" +
                "action=" + action +
                ", beanClass=" + beanClass +
                ", member='" + member + '\'' +
                ", value='" + value + '\'' +
                '}';
    }

    static JkColumnText toColumnText(List<EngineCommand> commands) {
        JkColumnText columnText = JkColumnText.ofSingle(1, 59)  // KBean
                .addColumn(1, 15)  // Bean
                .addColumn(1, 50)  // member
                .addColumn(1, 80);  // value
        List<EngineCommand> sortedCommands = commands.stream()
                .sorted(displayComparator()).collect(Collectors.toList());
        for (EngineCommand cmd : sortedCommands) {
            String member = cmd.action == Action.METHOD_INVOKE ? cmd.member + "()" : cmd.member;
            columnText.add(cmd.beanClass.getSimpleName(), cmd.action.name,
                    JkUtilsString.nullToEmpty(member), JkUtilsString.nullToEmpty(Objects.toString(cmd.value)));
        }
        return columnText;
    }

    private static Comparator<EngineCommand> displayComparator() {

        return (c1, c2) -> {
            if (c1.beanClass.equals(c2.beanClass)) {
                if (c1.action == Action.METHOD_INVOKE && c2.action != Action.METHOD_INVOKE) {
                    return 1;
                }
                if (c1.action != Action.METHOD_INVOKE && c2.action == Action.METHOD_INVOKE) {
                    return -1;
                }
            }
            return 0;
        };
    }
}
