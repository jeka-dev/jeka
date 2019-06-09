package dev.jeka.core;

import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkConstants;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/*
 * Utility class to build friendly HTML doc from markdown files.
 */
class DocMaker {

    private static final Charset UTF8 = Charset.forName("UTF8");

    private final JkPathTree docSource;

    private final Path docDist;

    private final Path htmlTemplates;

    private final String version;

    DocMaker(Path baseDir, Path distribPath, String version) {
        docSource = JkPathTree.of(baseDir.resolve("src/main/doc"));
        docDist = distribPath.resolve("doc");
        htmlTemplates = baseDir.resolve("jeka/doc-templates").toAbsolutePath().normalize();
        this.version = version;
    }

    void assembleAllDoc() {
        JkUtilsPath.createDirectories(docDist);
        JkPathTree.of(docDist).deleteContent();
        assembleHtmlDoc();
    }


    private void assembleHtmlDoc() {
        Path targetFolder = docDist;
        JkUtilsPath.createDirectories(targetFolder);
        docSource.andMatching(true,"*.md").getFiles().forEach(path -> {
            String content = new String(JkUtilsPath.readAllBytes(path), UTF8);
            String html = mdToHtml(content, JkUtilsString.substringBeforeLast(path.getFileName().toString(), "."));
            String name = path.getFileName().toString().replace(".md", ".html");
            JkUtilsPath.write(targetFolder.resolve(name), html.getBytes(UTF8));
        });
        String html = mdToHtml(createSingleReferenceMdPage(), "Reference Guide");
        JkUtilsPath.write(targetFolder.resolve("reference.html"), html.getBytes(Charset.forName("UTF8")));
        JkPathTree.of(htmlTemplates).andMatching("**/*.css", "**/*.jpg", "**/*.svg", "**/*.js")
                .copyTo(docDist.resolve("style"));
    }

    private String createSingleReferenceMdPage() {
        final StringBuilder sb = new StringBuilder();
        List<Path> paths = docSource.goTo("Reference Guide").getFiles();
        paths.sort((path1, path2) -> path1.compareTo(path2));
        for(Path path : paths) {
            String content = new String(JkUtilsPath.readAllBytes(path), Charset.forName("UTF8"));
            sb.append(content);
        }
        return sb.toString();
    }

    private String mdToHtml(String mdContent, String title) {
        StringBuilder sb = new StringBuilder();
        String rawHeader = new String(JkUtilsPath.readAllBytes(htmlTemplates.resolve("header.html")), UTF8);
        sb.append( rawHeader.replace("${title}", title).replace("${version}", version) );
        Parser parser = Parser.builder().build();
        Node document = parser.parse(mdContent);
        List<MenuItem> menuItems = addAnchorAndNumberingToHeaders(document);
        addMenu(document, menuItems);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        sb.append(renderer.render(document));
        sb.append( new String(JkUtilsPath.readAllBytes(htmlTemplates.resolve("footer.html")), UTF8) );
        return sb.toString();
    }

    public static void main(String[] args) {
        new DocMaker(Paths.get("."), Paths.get(JkConstants.OUTPUT_PATH + "/distrib"), "unspecified")
                .assembleAllDoc();
    }

    private List<MenuItem> addAnchorAndNumberingToHeaders(Node node) {
        List<MenuItem> menuItems = new LinkedList<>();
        int[] counters = new int[10];
        node.accept(new AbstractVisitor() {

            @Override
            public void visit(Heading heading) {
                Text text = (Text) heading.getFirstChild();
                String content = text.getLiteral();
                boolean intro = "Introduction".equals(content);  // Do not number Introduction
                if (!intro) {
                    counters[heading.getLevel()]++;
                    for (int i = heading.getLevel() + 1; i < 6; i++) {
                        counters[i] = 0;
                    }
                }
                StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= heading.getLevel(); i++) {
                    sb.append(counters[i]).append(".");
                }
                if (sb.length() > 1 && heading.getLevel() > 1) {
                   sb.delete(sb.length() - 1, sb.length() );
                }
                String number = sb.toString();
                String anchorId = content.replace(" ", "");
                HtmlInline htmlInline = new HtmlInline();
                htmlInline.setLiteral("<a name=\"" + anchorId + "\"></a>");
                heading.insertBefore(htmlInline);
                String numberedTitle = intro ? content : number + " " + content;
                String bulleteditle = heading.getLevel() < 3 ? content : "&bull;&nbsp;" + content;
                ////text.setLiteral(numberedTitle);
                MenuItem menuItem = new MenuItem(content, anchorId, heading.getLevel());
                menuItems.add(menuItem);
            }
        });



        return menuItems;
    }

    private void addMenu(Node document, List<MenuItem> menuItems) {
        List<MenuItem> reversedItems = new LinkedList<>(menuItems);
        Collections.reverse(reversedItems);
        for (MenuItem menuItem : reversedItems) {
            if (menuItem.level > 5) {
                continue;
            }
            Link link = new Link();
            link.setTitle(menuItem.title);
            Text text = new Text();
            text.setLiteral( menuItem.title);
            link.appendChild(text);
            link.setDestination("#" + menuItem.anchorId);
            HtmlInline indent = new HtmlInline();
            String cssClass = "menuItem" + menuItem.level;
            indent.setLiteral("<a href=\"#" + menuItem.anchorId + "\" class=\"" + cssClass + "\">" + menuItem.title + "</a>");
            document.prependChild(indent);
            document.prependChild(new HardLineBreak());
        }
    }

    static class MenuItem {

        final String title;

        final String anchorId;

        final int level;

        public MenuItem(String title, String anchorId, int level) {
            super();
            this.title = title;
            this.anchorId = anchorId;
            this.level = level;
        }

        @Override
        public String toString() {
            return title + "(" + level + ")";
        }

    }

}
