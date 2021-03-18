package dankmap.osm;

import dankmap.drawing.DrawType;
import org.junit.Test;

import javax.xml.stream.XMLStreamException;
import java.io.EOFException;
import java.io.File;
import java.util.InputMismatchException;

import static org.junit.Assert.*;

public class OSMParserTest {
    private final static String resourceLocation = "dankmap/osm/osmparsertest/";

    @Test
    public void testOSMFileFormat() {
        DrawType.loadDrawTypeMap();
        var file = getClass().getClassLoader().getResource(resourceLocation + "B1.osm").getFile();
        assertNoExceptionOnLoad(file);
    }

    // @Test
    // public void testZIPFileFormat() {
    //     DrawType.loadDrawTypeMap();
    //     var file = getClass().getClassLoader().getResource(resourceLocation + "B2.osm.zip").getFile();
    //     assertNoExceptionOnLoad(file);
    // }

    @Test
    public void testUnacceptedFileFormat() {
        var file = getClass().getClassLoader().getResource(resourceLocation + "B3.txt").getFile();
        assertExceptionOnLoad(file, InputMismatchException.class);
    }

    @Test
    public void testNullFile() {
        Exception e = null;
        try {
            new OSMParser(null).load();
        } catch (Exception exc) {
            e = exc;
        }
        assertNotNull(e);
        assertEquals(InputMismatchException.class, e.getClass());
    }

    @Test
    public void testNoContentFile() {
        var file = getClass().getClassLoader().getResource(resourceLocation + "B37.osm").getFile();
        assertExceptionOnLoad(file, XMLStreamException.class);
    }

    @Test
    public void testMissingOSMTag() {
        var file = getClass().getClassLoader().getResource(resourceLocation + "B5.osm").getFile();
        assertExceptionOnLoad(file, InputMismatchException.class);
    }

    @Test
    public void testSingleOSMTag() {
        var file = getClass().getClassLoader().getResource(resourceLocation + "B6.osm").getFile();
        assertNoExceptionOnLoad(file);
    }

    @Test
    public void testNoBoundsTag() {
        var file = getClass().getClassLoader().getResource(resourceLocation + "B7.osm").getFile();
        assertExceptionOnLoad(file, EOFException.class);
    }

    @Test
    public void testSingleBoundsTag() {
        var file = getClass().getClassLoader().getResource(resourceLocation + "B8.osm").getFile();
        assertNoExceptionOnLoad(file);
    }

    @Test
    public void testMultipleBoundsTag() {
        var file = getClass().getClassLoader().getResource(resourceLocation + "B9.osm").getFile();
        assertNoExceptionOnLoad(file);
    }

    @Test
    public void testNoMetaTags() {
        var file = getClass().getClassLoader().getResource(resourceLocation + "B10.osm").getFile();
        assertNoExceptionOnLoad(file);
    }

    @Test
    public void testSingleMetaTag() {
        var file = getClass().getClassLoader().getResource(resourceLocation + "B11.osm").getFile();
        assertNoExceptionOnLoad(file);
    }

    @Test
    public void testParseNode() {
        for (int i = 12; i < 18; i++) {
            var file = getClass().getClassLoader().getResource(resourceLocation + "B" + i + ".osm").getFile();
            assertNoExceptionOnLoad(file);
        }
    }

    // @Test
    // public void testParseWays() {
    //     for (int i = 18; i < 26; i++) {
    //         var file = getClass().getClassLoader().getResource(resourceLocation + "B" + i + ".osm").getFile();
    //         assertNoExceptionOnLoad(file);
    //     }
    // }

    @Test
    public void testParseRelations() {
        DrawType.loadDrawTypeMap();
        for (int i = 27; i < 37; i++) {
            var file = getClass().getClassLoader().getResource(resourceLocation + "B" + i + ".osm").getFile();
            assertNoExceptionOnLoad(file);
        }
    }

    private static void assertExceptionOnLoad(String file, Class excType) {
        Exception e = null;
        try {
            new OSMParser(new File(file)).load();
        } catch (Exception exc) {
            e = exc;
        }
        assertNotNull(e);
        assertEquals(excType, e.getClass());
    }

    private static void assertNoExceptionOnLoad(String file) {
        Exception e = null;
        try {
            new OSMParser(new File(file)).load();
        } catch (Exception exc) {
            e = exc;
        }
        assertNull(e);
    }
}