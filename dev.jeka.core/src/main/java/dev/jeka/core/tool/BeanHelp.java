package dev.jeka.core.tool;

import dev.jeka.core.api.utils.JkUtilsReflect;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Gives the description of a KBean : its name, its purpose and its base class.
 *
 * @author Jerome Angibaud
 */
// TODO maybe to merge with BeanDescription
class BeanHelp implements Comparable<BeanHelp> {

    private static String longName(Class<?> clazz) {
        return clazz.getName();
    }

    private final String shortName;

    private final String fullName;

    private final Class<? extends JkBean> clazz;

    BeanHelp(Class<? extends JkBean> clazz) {
        super();
        this.shortName = JkBean.computeShortName(clazz);
        this.fullName = longName(clazz);
        this.clazz = clazz;
    }

    public List<String> pluginDependencies() {
        List<String> result = new LinkedList<>();
        JkDocJkBeanDeps pluginDeps = clazz.getAnnotation(JkDocJkBeanDeps.class);
        if (pluginDeps == null) {
            return Collections.emptyList();
        }
        for (Class<?> depClass : pluginDeps.value()) {
            result.add(depClass.getName());
        }
        return result;
    }

    public String shortName() {
        return this.shortName;
    }

    public String fullName() {
        return this.fullName;
    }

    public Class<? extends JkBean> beanClass() {
        return clazz;
    }

    public List<String> description() {
        if (this.clazz.getAnnotation(JkDoc.class) == null) {
            return Collections.emptyList();
        }
        return Arrays.asList(this.clazz.getAnnotation(JkDoc.class).value());
    }

    public String shortDescription() {
        List<String> description = description();
        return description.isEmpty() ? null : description.get(0);
    }

    public List<String> activationEffect() {
        JkDoc doc = JkUtilsReflect.getInheritedAnnotation(clazz, JkDoc.class, "activate");
        return doc == null ? Collections.emptyList() : Arrays.asList(doc.value());
    }

    boolean isDecoratedBeanDefined() {
        Method decorateRun = JkUtilsReflect.findMethodMethodDeclaration(clazz, "activate");
        return decorateRun != null && !decorateRun.getDeclaringClass().equals(JkBean.class);
    }

    @Override
    public String toString() {
        return "name=" + this.shortName + "(" + this.fullName + ")";
    }

    @Override
    public int compareTo(BeanHelp o) {
        return this.shortName.compareTo(o.shortName);
    }
}
