package dev.jeka.core.tool;

import dev.jeka.core.api.text.JkColumnText;
import dev.jeka.core.api.utils.JkUtilsAssert;
import dev.jeka.core.api.utils.JkUtilsString;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

class EngineCommand {

    enum Action {

        SET_FIELD_VALUE("set-value"), INVOKE("invoke"), BEAN_INIT("initialize");

        private final String name;

        Action(String name) {
            this.name = name;
        }
    }

    private final Action action;

    private final Class<? extends KBean> beanClass;

    private final String member;

    private final Object value;  // for properties only

    private final String source;

    EngineCommand(Action action, Class<? extends KBean> beanClass, String valueOrMethod, Object value, String source) {
        JkUtilsAssert.argument(beanClass != null, "KBean class cannot be null.");
        this.action = action;
        this.beanClass = beanClass;
        this.member = valueOrMethod;
        this.value = value;
        this.source = source;
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
                .addColumn(1, 80)  // value
                .addColumn(1, 16); // source
        List<EngineCommand> sortedCommands = commands.stream()
                .sorted(displayComparator()).collect(Collectors.toList());
        for (EngineCommand cmd : sortedCommands) {
            String member = cmd.action == Action.INVOKE ? cmd.member + "()" : cmd.member;
            columnText.add(
                    cmd.beanClass.getSimpleName(),
                    cmd.action.name,
                    JkUtilsString.nullToEmpty(member),
                    cmd.value == null ? "" : cmd.value.toString(),
                    JkUtilsString.nullToEmpty(cmd.source)
            );
        }
        return columnText;
    }

    private static Comparator<EngineCommand> displayComparator() {

        return (c1, c2) -> {
            if (c1.beanClass.equals(c2.beanClass)) {
                if (c1.action == Action.INVOKE && c2.action != Action.INVOKE) {
                    return 1;
                }
                if (c1.action != Action.INVOKE && c2.action == Action.INVOKE) {
                    return -1;
                }
            }
            return 0;
        };
    }
}
