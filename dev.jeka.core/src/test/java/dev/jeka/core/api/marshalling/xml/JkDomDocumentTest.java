package dev.jeka.core.api.marshalling.xml;

import org.junit.Ignore;
import org.junit.Test;

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

}