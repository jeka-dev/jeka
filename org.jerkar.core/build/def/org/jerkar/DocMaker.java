package org.jerkar;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jerkar.api.file.JkPathTree;
import org.jerkar.api.utils.JkUtilsPath;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

class DocMaker {

    private static final Charset UTF8 = Charset.forName("UTF8");

    private final JkPathTree docSource;

    private final Path docDist;

    DocMaker(Path baseDir, Path distribPath) {
        baseDir = baseDir;
        docSource = JkPathTree.of(baseDir.resolve("src/main/doc"));
        docDist = distribPath.resolve("doc");
    }

    void assembleAllDoc() {
        JkUtilsPath.createDirectories(docDist);
        JkPathTree.of(docDist).deleteContent();
        assembleMdDoc();
        assembleHtmlDoc();
    }

    void assembleMdDoc() {
        Path targetFolder = docDist.resolve("markdown");
        JkUtilsPath.createDirectories(targetFolder);
        docSource.accept("*.md").copyTo(targetFolder);
    }

    void assembleHtmlDoc() {
        Path targetFolder = docDist.resolve("html");
        JkUtilsPath.createDirectories(targetFolder);
        Parser parser = Parser.builder().build();
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        docSource.accept("*.md").files().forEach(path -> {
            String content = new String(JkUtilsPath.readAllBytes(path), UTF8);
            String html = mdToHtml(content);
            String name = path.getFileName().toString().replace(".md", ".html");
            JkUtilsPath.write(targetFolder.resolve(name), html.getBytes(UTF8));
        });
        String html = mdToHtml(createSingleReferenceMdPage());
        JkUtilsPath.write(targetFolder.resolve("reference.html"), html.getBytes(Charset.forName("UTF8")));
        docSource.goTo("templates").accept("**/*.css").copyTo(docDist.resolve("html"));
    }

    private String createSingleReferenceMdPage() {
        final StringBuilder sb = new StringBuilder();
        docSource.goTo("reference").files().forEach(path -> {
            String content = new String(JkUtilsPath.readAllBytes(path), Charset.forName("UTF8"));
            sb.append(content);
        });
        return sb.toString();
    }

    private String mdToHtml(String mdContent) {
        StringBuilder sb = new StringBuilder();
        sb.append( new String(JkUtilsPath.readAllBytes(docSource.get("templates/header.html")), UTF8) );
        Parser parser = Parser.builder().build();
        Node document = parser.parse(mdContent);
        HtmlRenderer renderer = HtmlRenderer.builder().build();
        sb.append(renderer.render(document));
        sb.append( new String(JkUtilsPath.readAllBytes(docSource.get("templates/footer.html")), UTF8) );
        return sb.toString();
    }

    public static void main(String[] args) {
        new DocMaker(Paths.get("."), Paths.get("build/output/distrib")).assembleAllDoc();
    }

}
