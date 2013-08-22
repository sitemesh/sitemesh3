package org.sitemesh.config;

import junit.framework.TestCase;

/**
 * @author Joe Walnes
 * @author Mike Cannon-Brookes
 * @author Hani Suleiman
 */
public class PathMapperTest extends TestCase {

    private PathMapper<String> pathMapper;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        pathMapper = new PathMapper<String>();

        // exact matches come first
        pathMapper.put("/myexactfile.html", "exact1");
        pathMapper.put("/mydir/myexactfile.html", "exact2");
        pathMapper.put("/mydir/myexactfile.jsp", "exact3");
        pathMapper.put("/mydir/dodo", "exact4");

        // then the complex matches
        pathMapper.put("/mydir/*", "complex1");
        pathMapper.put("/mydir/otherdir/*.jsp", "complex2");
        pathMapper.put("/otherdir/*.??p", "complex3");
        pathMapper.put("*.xml", "complex4");
        pathMapper.put("/*/admin/*.??ml", "complex5");
        pathMapper.put("/*/complexx/a*b.x?tml", "complex6");

        // if all the rest fails, use the default matches
        pathMapper.put("*", "default");
    }

    public void testHardening() throws Exception {
        pathMapper = new PathMapper<String>();
        pathMapper.put(null, null);
        assertNull(pathMapper.get(null));
        assertNull(pathMapper.get(""));
        assertNull(pathMapper.get("/somenonexistingpath"));
    }
    
    public void testGetPatternInUse() throws Exception {
        assertEquals("/myexactfile.html",   pathMapper.getPatternInUse("/myexactfile.html"));
        assertEquals("/*/admin/*.??ml",     pathMapper.getPatternInUse("/blah/admin/myexactfile.gzml"));
        assertEquals("*",                   pathMapper.getPatternInUse("/blah.txt"));
    }
    
    public void testKeyTypeChecks() throws Exception {
        assertTrue(PathMapper.isComplexKey("/admin/*"));
        assertFalse(PathMapper.isComplexKey("/admin/my.file"));
        assertTrue(PathMapper.isDefaultKey("/*"));
        assertFalse(PathMapper.isComplexKey("/admin/my.file"));
        
        assertTrue(PathMapper.isMoreSpecific("*", "/*"));
        assertTrue(PathMapper.isMoreSpecific("/admin/*", "/*"));
        assertTrue(PathMapper.isMoreSpecific("/admin/my.file", "/admin/*"));
        assertTrue(PathMapper.isMoreSpecific("/admin/*", "/*.xml"));
        assertTrue(PathMapper.isMoreSpecific("/admin/my.file", "/admin/*"));
        
        assertFalse(PathMapper.isMoreSpecific("*.xml", "/admin/*.xml"));
        
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