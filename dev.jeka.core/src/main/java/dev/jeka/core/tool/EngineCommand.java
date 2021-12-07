package dev.jeka.core.tool;

class EngineCommand {

    enum Action {
        PROPERTY_INJECT, METHOD_INVOKE, BEAN_REGISTRATION
    }

    private Action action;

    private Class<? extends JkBean> beanClass;

    private String member;

    private String value;  // for properties only

    EngineCommand(Action action, Class<? extends JkBean> beanClass, String valueOrMethod, String value) {
        this.action = action;
        this.beanClass = beanClass;
        this.member = valueOrMethod;
        this.value = value;
    }

    public Action getAction() {
        return action;
    }

    public Class<? extends JkBean> getBeanClass() {
        return beanClass;
    }

    public String getMember() {
        return member;
    }

    public String getValue() {
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
}
