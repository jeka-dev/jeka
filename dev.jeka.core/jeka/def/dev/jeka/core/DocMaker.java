package dev.jeka.core;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.utils.JkUtilsPath;
import dev.jeka.core.api.utils.JkUtilsString;
import dev.jeka.core.tool.JkConstants;
import org.commonmark.node.*;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

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
        htmlTemplates = docSource.getRoot().resolve("templates").toAbsolutePath().normalize();
        this.version = version;
    }

    void assembleAllDoc() {
        JkUtilsPath.createDirectories(docDist);
        JkPathTree.of(docDist).deleteContent();
        assembleHtmlDoc();
    }

    private void assembleHtmlDoc() {
        final Path targetFolder = docDist;
        JkUtilsPath.createDirectories(targetFolder);
        docSource.andMatching(true,"*.md").getFiles().forEach(path -> {
            final String content = new String(JkUtilsPath.readAllBytes(path), UTF8);
            final String html = mdToHtml(content, JkUtilsString.substringBeforeLast(path.getFileName().toString(), "."));
            final String name = path.getFileName().toString().replace(".md", ".html");
            JkUtilsPath.write(targetFolder.resolve(name), html.getBytes(UTF8));
        });
    }

    private String mdToHtml(String mdContent, String title) {
        final StringBuilder sb = new StringBuilder();
        final String rawHeader = new String(JkUtilsPath.readAllBytes(htmlTemplates.resolve("header.html")), UTF8);
        sb.append( rawHeader.replace("${title}", title).replace("${version}", version) );
        final Parser parser = Parser.builder().build();
        final Node document = parser.parse(mdContent);
        final List<MenuItem> menuItems = addAnchorAndNumberingToHeaders(document);
        addMenu(document, menuItems);
        final HtmlRenderer renderer = HtmlRenderer.builder().build();
        sb.append(renderer.render(document));
        sb.append( new String(JkUtilsPath.readAllBytes(htmlTemplates.resolve("footer.html")), UTF8) );
        return sb.toString();
    }

    public static void main(String[] args) {
        new DocMaker(Paths.get(""), Paths.get(JkConstants.OUTPUT_PATH + "/distrib"), "unspecified")
        .assembleAllDoc();
    }

    private List<MenuItem> addAnchorAndNumberingToHeaders(Node node) {
        final List<MenuItem> menuItems = new LinkedList<>();
        final int[] counters = new int[10];
        node.accept(new AbstractVisitor() {

            @Override
            public void visit(Heading heading) {
                final Text text = (Text) heading.getFirstChild();
                final String content = text.getLiteral();
                final boolean intro = "Introduction".equals(content);  // Do not number Introduction
                if (!intro) {
                    counters[heading.getLevel()]++;
                    for (int i = heading.getLevel() + 1; i < 6; i++) {
                        counters[i] = 0;
                    }
                }
                final StringBuilder sb = new StringBuilder();
                for (int i = 1; i <= heading.getLevel(); i++) {
                    sb.append(counters[i]).append(".");
                }
                if (sb.length() > 1 && heading.getLevel() > 1) {
                    sb.delete(sb.length() - 1, sb.length() );
                }
                final String anchorId = content.replace(" ", "");
                final HtmlInline htmlInline = new HtmlInline();
                htmlInline.setLiteral("<a name=\"" + anchorId + "\"></a>");
                heading.insertBefore(htmlInline);
                final MenuItem menuItem = new MenuItem(content, anchorId, heading.getLevel());
                menuItems.add(menuItem);
            }
        });
        return menuItems;
    }

    private void addMenu(Node document, List<MenuItem> menuItems) {
        final List<MenuItem> reversedItems = new LinkedList<>(menuItems);
        Collections.reverse(reversedItems);
        for (final MenuItem menuItem : reversedItems) {
            if (menuItem.level > 5) {
                continue;
            }
            final Link link = new Link();
            link.setTitle(menuItem.title);
            final Text text = new Text();
            text.setLiteral( menuItem.title);
            link.appendChild(text);
            link.setDestination("#" + menuItem.anchorId);
            final HtmlInline indent = new HtmlInline();
            final String cssClass = "menuItem" + menuItem.level;
            String prefix = JkUtilsString.repeat("&nbsp;", menuItem.level * 6);
            indent.setLiteral(prefix + "<a href=\"#" + menuItem.anchorId + "\" class=\"" + cssClass + "\">" + menuItem.title + "</a>");
            document.prependChild(indent);
            document.prependChild(new HardLineBreak());
        }
    }


    private static class MenuItem {

        final String title;

        final String anchorId;

        final int level;

        MenuItem(String title, String anchorId, int level) {
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
