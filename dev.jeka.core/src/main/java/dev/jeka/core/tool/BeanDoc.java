package dev.jeka.core.tool;

import dev.jeka.core.api.utils.JkUtilsReflect;
import dev.jeka.core.api.utils.JkUtilsString;
import org.apache.ivy.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Gives the description of a KBean : its name, its purpose and its base class.
 *
 * @author Jerome Angibaud
 */
// TODO maybe to merge with BeanDescription
class BeanDoc implements Comparable<BeanDoc> {

    private static String longName(Class<?> clazz) {
        return clazz.getName();
    }

    private final String shortName;

    private final String fullName;

    private final Class<? extends JkBean> clazz;

    BeanDoc(Class<? extends JkBean> clazz) {
        super();
        this.shortName = JkBean.name(clazz);
        this.fullName = longName(clazz);
        this.clazz = clazz;
    }

    public List<String> pluginDependencies() {
        return JkUtilsReflect.getAllDeclaredFields(clazz, false).stream()
                .filter(field -> JkBean.class.isAssignableFrom(field.getType()))
                .map(Field::getType)
                .map(Class::getName)
                .collect(Collectors.toList());
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
        if (description.isEmpty()) {
            return null;
        }
        String result = description.get(0);
        return result.contains("\n") ?  JkUtilsString.substringBeforeFirst(result, "\n") : result;
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
    public int compareTo(BeanDoc o) {
        return this.shortName.compareTo(o.shortName);
    }
}
