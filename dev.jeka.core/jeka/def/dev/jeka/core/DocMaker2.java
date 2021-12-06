package dev.jeka.core;

import dev.jeka.core.api.file.JkPathTree;
import dev.jeka.core.api.marshalling.JkDomDocument;
import dev.jeka.core.api.marshalling.JkDomElement;
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
class DocMaker2 {

    private static final Charset UTF8 = Charset.forName("UTF8");

    private final JkPathTree docSourceDir;

    private final Path targetDir;

    private final Path templateDir;

    DocMaker2(Path baseDir, Path distribPath) {
        docSourceDir = JkPathTree.of(baseDir.resolve("src/main/doc"));
        targetDir = distribPath.resolve("doc");
        templateDir = docSourceDir.getRoot().resolve("templates").toAbsolutePath().normalize();
    }

    private void make() {
        JkPathTree.of(targetDir).createIfNotExist().deleteContent();
        docSourceDir.andMatching(true,"*.md").getFiles().forEach(path -> {
            final String content = new String(JkUtilsPath.readAllBytes(path), UTF8);
            final String html = mdToHtml(content, JkUtilsString.substringBeforeLast(path.getFileName().toString(), "."));
            final String name = path.getFileName().toString().replace(".md", ".html");
            JkUtilsPath.write(targetDir.resolve(name), html.getBytes(UTF8));
        });
    }

    private String mdToHtml(String mdContent, String title) {
        final StringBuilder sb = new StringBuilder();
        final String rawHeader = new String(JkUtilsPath.readAllBytes(templateDir.resolve("header.html")), UTF8);
        sb.append( rawHeader.replace("${title}", title) );
        final Parser parser = Parser.builder().build();
        final Node document = parser.parse(mdContent);
        final List<MenuItem> menuItems = addAnchorAndNumberingToHeaders(document);
        addMenu(document, menuItems);
        final HtmlRenderer renderer = HtmlRenderer.builder().build();
        sb.append(renderer.render(document));
        sb.append( new String(JkUtilsPath.readAllBytes(templateDir.resolve("footer.html")), UTF8) );
        return sb.toString();
    }

    public static void main(String[] args) {
        new DocMaker2(Paths.get(""), Paths.get(JkConstants.OUTPUT_PATH + "/distrib"), "unspecified")
        .assembleAllDoc();
    }

    JkDomElement menu(Node mdDocumet) {
        JkDomElement menu = JkDomDocument.of("menu").root();
        JkDomElement currentElement = menu;
        int previousLevel = 0;
        mdDocumet.accept(new AbstractVisitor() {

            @Override
            public void visit(Heading heading) {
                final Text text = (Text) heading.getFirstChild();
                final String content = text.getLiteral();
                final String anchorId = content.replace(" ", "");
                if (heading.getLevel() > previousLevel) {
                    menu.add("menuItem")
                            .add("a").attr("name", anchorId);
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
            indent.setLiteral("<a href=\"#" + menuItem.anchorId + "\" class=\"" + cssClass + "\">" + menuItem.title + "</a>");
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
