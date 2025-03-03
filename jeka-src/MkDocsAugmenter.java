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

/*
 * Include inferred documentation (from @JkDoc, @JkPostInt,...)  in KBeans md doc.
 */
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

        String genContent = JkBeanDescription.of(clazz).toMdContent();
        String newFileContent = replace(fileContent, genContent);

        docPathFile.deleteIfExist().createIfNotExist().write(newFileContent);
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
