package org.sitemesh.html;

import junit.framework.Test;
import junit.framework.TestSuite;

import java.io.IOException;

import org.sitemesh.content.tagrules.html.CoreHtmlTagRuleBundle;
import org.sitemesh.content.tagrules.msoffice.MsOfficeTagRuleBundle;
import org.sitemesh.content.tagrules.TagBasedContentProcessor;

public class MsOfficeHtmlContentProcessorTest {

    /**
     * This test case builds a custom suite, containing a collection of smaller suites
     * (one for each file of testdata/text??.txt).
     */
    public static Test suite() throws IOException {
        TestSuite suite = new TestSuite(MsOfficeHtmlContentProcessorTest.class.getName());
        DataDrivenSuiteBuilder.buildSuite(
                suite,
                new TagBasedContentProcessor(new CoreHtmlTagRuleBundle(), new MsOfficeTagRuleBundle()),
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
                // "test22.txt", Unsupported SM2 features.
                // "test23.txt", Unsupported SM2 features.
                "test24.txt",
                // "test25.txt", Unsupported SM2 features.
                "test26.txt",
                "test27.txt",
                "test28.txt",
                // "test29.txt", Unsupported SM2 features.
                // "test30.txt",
                "test31.txt",
                // "test32.txt", Unsupported SM2 features.
                // "test33.txt", Unsupported SM2 features.
                "test34.txt",
                // "test35.txt", Unsupported SM2 features.
                "test36.txt",
                "test37.txt",
                "test38.txt", // <-- Specific MSOffice test.
                "test39.txt",
                // "test40.txt", Unsupported SM2 features.
                "test41.txt",
                // "test42.txt", Unsupported SM2 features.
                "test43.txt"
                // "test44.txt" Unsupported SM2 features.
        );
        return suite;
    }

}