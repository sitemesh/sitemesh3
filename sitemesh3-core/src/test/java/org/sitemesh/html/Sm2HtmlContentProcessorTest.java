package org.sitemesh.html;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.IOException;

import org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle;
import org.sitemesh.content.tagrules.html.Sm2TagRuleBundle;
import org.sitemesh.content.tagrules.TagBasedContentProcessor;

/**
 * This is a data driven test suite. See testdata/readme.txt.
 *
 * @author Joe Walnes
 */
public class Sm2HtmlContentProcessorTest {

    public static Test suite() throws IOException {
        TestSuite suite = new TestSuite(Sm2HtmlContentProcessorTest.class.getName());
        DataDrivenSuiteBuilder.buildSuite(
                suite,
                new TagBasedContentProcessor(new CoreHtmlTagRuleBundle(), new Sm2TagRuleBundle()),
                "test01.txt",
                "test02.txt",
                "test03.txt",
                "test04.txt",
                "test05.txt",
                "test06.txt",
                "test07.txt",
                "test08.txt",
                "test09.txt",
                "test10.txt",
                "test11.txt",
                "test12.txt",
                "test13.txt",
                "test14.txt",
                "test15.txt",
                "test16.txt",
                "test17.txt",
                "test18.txt",
                "test19.txt",
                "test20.txt",
                "test21.txt",
                "test22.txt",
                "test23.txt",
                "test24.txt",
                "test25.txt",
                "test26.txt",
                "test27.txt",
                "test28.txt",
                "test29.txt",
                "test30.txt",
                "test31.txt",
                "test32.txt",
                "test33.txt",
                "test34.txt",
                "test35.txt",
                "test36.txt",
                "test37.txt",
                // "test38.txt", This uses MSOffice properties, which has been moved to MsOfficeTagRuleBundle.
                "test39.txt",
                "test40.txt",
                "test41.txt",
                "test42.txt",
                "test43.txt",
                "test44.txt"
        );
        return suite;
    }

}