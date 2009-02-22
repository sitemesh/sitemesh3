package com.opensymphony.sitemesh.decorator.map;

import junit.framework.TestCase;

/**
 * @author Joe Walnes
 * @author Mike Cannon-Brookes
 * @author Hani Suleiman
 */
public class PathMapperTest extends TestCase {

    private PathMapper pathMapper;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        pathMapper = new PathMapper();

        // exact matches come first
        pathMapper.put("exact1", "/myexactfile.html");
        pathMapper.put("exact2", "/mydir/myexactfile.html");
        pathMapper.put("exact3", "/mydir/myexactfile.jsp");
        pathMapper.put("exact4", "/mydir/dodo");

        // then the complex matches
        pathMapper.put("complex1", "/mydir/*");
        pathMapper.put("complex2", "/mydir/otherdir/*.jsp");
        pathMapper.put("complex3", "/otherdir/*.??p");
        pathMapper.put("complex4", "*.xml");
        pathMapper.put("complex5", "/*/admin/*.??ml");
        pathMapper.put("complex6", "/*/complexx/a*b.x?tml");

        // if all the rest fails, use the default matches
        pathMapper.put("default", "*");
    }

    public void testHardening() throws Exception {
        PathMapper bad = new PathMapper();
        bad.put(null, null);
        assertNull(bad.get(null));
        assertNull(bad.get(""));
        assertNull(bad.get("/somenonexistingpath"));
    }

    public void testFindExactKey() throws Exception {
        assertEquals("exact1", pathMapper.get("/myexactfile.html"));
        assertEquals("exact2", pathMapper.get("/mydir/myexactfile.html"));
        assertEquals("exact3", pathMapper.get("/mydir/myexactfile.jsp"));
        assertEquals("exact4", pathMapper.get("/mydir/dodo"));
    }

    public void testFindComplexKey() throws Exception {
        assertEquals("complex1", pathMapper.get("/mydir/"));
        assertEquals("complex1", pathMapper.get("/mydir/test1.xml"));
        assertEquals("complex1", pathMapper.get("/mydir/test321.jsp"));
        assertEquals("complex1", pathMapper.get("/mydir/otherdir"));

        assertEquals("complex2", pathMapper.get("/mydir/otherdir/test321.jsp"));

        assertEquals("complex3", pathMapper.get("/otherdir/test2.jsp"));
        assertEquals("complex3", pathMapper.get("/otherdir/test2.bpp"));

        assertEquals("complex4", pathMapper.get("/somedir/one/two/some/deep/file/test.xml"));
        assertEquals("complex4", pathMapper.get("/somedir/321.jsp.xml"));

        assertEquals("complex5", pathMapper.get("/mydir/otherdir/admin/myfile.html"));
        assertEquals("complex5", pathMapper.get("/mydir/somedir/admin/text.html"));

        assertEquals("complex6", pathMapper.get("/mydir/complexx/a-some-test-b.xctml"));
        assertEquals("complex6", pathMapper.get("/mydir/complexx/a b.xhtml"));
        assertEquals("complex6", pathMapper.get("/mydir/complexx/a___b.xhtml"));
    }

    public void testFindDefaultKey() throws Exception {
        assertEquals("default", pathMapper.get(null));
        assertEquals("default", pathMapper.get("/"));
        assertEquals("default", pathMapper.get("/*"));
        assertEquals("default", pathMapper.get("*"));
        assertEquals("default", pathMapper.get("blah.txt"));
        assertEquals("default", pathMapper.get("somefilewithoutextension"));
        assertEquals("default", pathMapper.get("/file_with_underscores-and-dashes.test"));
        assertEquals("default", pathMapper.get("/tuuuu*/file.with.dots.test.txt"));
    }
}