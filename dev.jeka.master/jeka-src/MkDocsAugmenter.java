/*
 * Copyright 2014-2025  the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 *  limitations under the License.
 */

import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkBeanDescription;
import dev.jeka.core.tool.KBean;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

class MkDocsAugmenter {

    private static final String PLACE_HOLDER = Matcher.quoteReplacement("<!-- autogen-doc -->");

    private final Path docDir;

     MkDocsAugmenter(Path docDir) {
        this.docDir = docDir;
    }

    void perform() {
        JkBeanDescription.STANDARD_KBEAN_CLASSES.forEach(this::perform);
    }

    private void perform(Class<? extends KBean> clazz) {
        String simpleClassName = clazz.getSimpleName();
        String beanName = JkUtilsString.substringBeforeLast(simpleClassName, "KBean");
        beanName = JkUtilsString.uncapitalize(beanName);
        Path docFileName = docDir.resolve("reference/kbeans-" + beanName + ".md");

        JkPathFile docPathFile = JkPathFile.of(docFileName);
        String fileContent = docPathFile.readAsString();

        String genContent = mdContent(JkBeanDescription.of(clazz));
        String newFileContent = replace(fileContent, genContent);

        docPathFile.deleteIfExist().createIfNotExist().write(newFileContent);
    }

    private String mdContent(JkBeanDescription beanDescription) {
        StringBuilder sb = new StringBuilder();
        sb.append(beanDescription.synopsisHeader).append("\n\n");
        sb.append(beanDescription.synopsisDetail).append("\n\n");
        sb.append("|Field  |Description  |Type  |\n");
        sb.append("|-------|-------------|------|\n");
        beanDescription.beanFields.forEach(field -> sb.append(fieldContent(field)));

        sb.append("\n\n");
        sb.append("|Method  |Description  |\n");
        sb.append("|--------|-------------|\n");
        beanDescription.beanMethods.forEach(method -> sb.append(methodContent(method)));

        return sb.toString();
    }

    private String fieldContent(JkBeanDescription.BeanField beanField) {
        return String.format("|%s |%s |%s |%n",
                beanField.name,
                oneLiner(beanField.description),
                JkUtilsString.removePackagePrefix(beanField.type.getName()));
    }

    private String methodContent(JkBeanDescription.BeanMethod beanMethod) {
        return String.format("|%s |%s |%n",
                beanMethod.name,
                oneLiner(beanMethod.description));
    }

    private static String oneLiner(String original) {
         return original.replaceAll("\\n", "<br/>").replaceAll("%n", "<br/>");
    }

    private static String replace(String original, String replacement) {
        List<String> result = new LinkedList<>();
        List<String> lines = Arrays.asList(original.split("\n"));
        for (String line : lines) {
            if (line.trim().equals(PLACE_HOLDER)) {
                result.add(replacement);
            } else {
                result.add(line);
            }
        }
        return String.join("\n", result);
    }
}
