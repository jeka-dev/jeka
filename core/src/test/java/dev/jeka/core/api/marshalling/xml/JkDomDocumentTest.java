package dev.jeka.core.api.marshalling.xml;

import dev.jeka.core.api.utils.JkUtilsPath;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.nio.file.Path;

public class JkDomDocumentTest {

    @Test
    @Ignore
    public void print() {
        JkDomDocument doc = JkDomDocument.parse(JkDomDocumentTest.class.getResourceAsStream("sample.xml"));
        doc.root()
                .child("component")
                    .child("modules")
                            .add("module").attr("filepath", "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa").attr("fileurl", "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb").make();
        System.out.println(doc.toXml());
        doc.print(System.out);
    }

    @Test
    public void modifyAndSave() {
        JkDomDocument doc = JkDomDocument.parse(JkDomDocumentTest.class.getResourceAsStream("sample.xml"));
        Path temp = JkUtilsPath.createTempFile("jktest", ".xml");
        doc.root().child("component/version").text("2")
                        .getDoc().save(temp);
        Assert.assertEquals("2", JkDomDocument.parse(temp).root().child("component/version").text());
    }



}