import dev.jeka.core.api.file.JkPathFile;
import dev.jeka.core.api.system.JkLog;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkBeanDescription;
import dev.jeka.core.tool.KBean;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;

/*
 * Include inferred documentation (from @JkDoc, @JkPostInt,...)  in KBeans MkDocs.
 */
class MkDocsEnricher {

    private static final String HEADER_PLACE_HOLDER = Matcher.quoteReplacement("<!-- header-autogen-doc -->");

    private static final String BODY_PLACE_HOLDER = Matcher.quoteReplacement("<!-- body-autogen-doc -->");

    private final Path docDir;

     MkDocsEnricher(Path docDir) {
        this.docDir = docDir;
    }

    void run() {
        JkBeanDescription.STANDARD_KBEAN_CLASSES.forEach(this::run);
        JkLog.info("Documents generated in: %s", docDir);
    }

    private void run(Class<? extends KBean> clazz) {
        String simpleClassName = clazz.getSimpleName();
        String beanName = JkUtilsString.substringBeforeLast(simpleClassName, "KBean");
        beanName = JkUtilsString.uncapitalize(beanName);
        Path docFileName = docDir.resolve("reference/kbeans-" + beanName + ".md");

        JkPathFile docPathFile = JkPathFile.of(docFileName);
        String fileContent = docPathFile.readAsString();

        JkBeanDescription.MdContent genContent = JkBeanDescription.of(clazz).toMdContent();
        String newFileContent = replace(fileContent, HEADER_PLACE_HOLDER, genContent.header);
        newFileContent = replace(newFileContent, BODY_PLACE_HOLDER, genContent.body);

        docPathFile.deleteIfExist().createIfNotExist().write(newFileContent);
    }

    private static String replace(String original, String placeHolder, String replacement) {
        List<String> result = new LinkedList<>();
        List<String> lines = Arrays.asList(original.split("\n"));
        for (String line : lines) {
            if (line.trim().equals(placeHolder)) {
                result.add(replacement);
            } else {
                result.add(line);
            }
        }
        return String.join("\n", result);
    }
}
