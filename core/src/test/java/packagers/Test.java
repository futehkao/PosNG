package packagers;

import com.futeh.posng.iso.ISOMsg;
import com.futeh.posng.iso.ISOPackager;
import com.futeh.posng.iso.packager.*;
import iso.TestUtils;
import java.io.*;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Simple 'HelloWorld' like TestCase
 * used to play with jUnit and to verify
 * build/junit system is running OK.
 */
public class Test  {
    private XMLPackager xmlPackager;
    public static final String PREFIX = "src/test/java/packagers/";

    private ISOMsg getMsg (String message) throws Exception {
        FileInputStream fis = new FileInputStream (PREFIX + message + ".xml");
        byte[] b = new byte[fis.available()];
        fis.read (b);
        ISOMsg m = new ISOMsg ();
        m.setPackager (xmlPackager);
        m.unpack (b);
        fis.close();
        return m;
    }
    private byte[] getImage (String message) throws Exception {
        FileInputStream fis = new FileInputStream (PREFIX + message + ".bin");
        byte[] b = new byte[fis.available()];
        fis.read (b);
        fis.close();
        return b;
    }

    private void writeImage (String message, byte[] b) throws Exception {
        FileOutputStream fos = new FileOutputStream (PREFIX + message + ".run");
        fos.write (b);
        fos.close();
    }

    @BeforeEach
    public void setUp () throws Exception {
        xmlPackager = new XMLPackager();
    }
    @org.junit.jupiter.api.Test
    public void testPostPackager () throws Exception {
        doTest (new PostPackager(), "post", "post");
    }
    @org.junit.jupiter.api.Test
    public void testISO87APackager() throws Exception {
        doTest (new ISO87APackager(), "ISO87", "ISO87APackager");
    }
    @org.junit.jupiter.api.Test
    public void testISO87BPackager() throws Exception {
        doTest (new ISO87BPackager(), "ISO87", "ISO87BPackager");
    }
    @org.junit.jupiter.api.Test
    public void testGeneric87ascii() throws Exception {
        doTest (new GenericPackager("src/config/packager/iso87ascii.xml"),
            "ISO87", "ISO87APackager");
    }
    @org.junit.jupiter.api.Test
    public void testGeneric87binary() throws Exception {
        doTest (new GenericPackager ("src/config/packager/iso87binary.xml"),
            "ISO87", "ISO87BPackager");
    }
    @org.junit.jupiter.api.Test
    public void testISO93APackager() throws Exception {
        doTest (new ISO93APackager(), "ISO93", "ISO93APackager");
    }
    @org.junit.jupiter.api.Test
    public void testISO93BPackager() throws Exception {
        doTest (new ISO93BPackager(), "ISO93", "ISO93BPackager");
    }
    @org.junit.jupiter.api.Test
    public void testGeneric93ascii() throws Exception {
        doTest (new GenericPackager ("src/config/packager/iso93ascii.xml"),
            "ISO93", "ISO93APackager");
    }
    @org.junit.jupiter.api.Test
    public void testGeneric93binary() throws Exception {
        doTest (new GenericPackager ("src/config/packager/iso93binary.xml"),
            "ISO93", "ISO93BPackager");
    }
    @org.junit.jupiter.api.Test
    public void testF64Binary() throws Exception {
        doTest (new GenericPackager ("src/config/packager/iso87binary.xml"),
            "ISO87-Field64", "ISO87B-Field64");
    }
    @org.junit.jupiter.api.Test
    public void testF64ascii() throws Exception {
        doTest (new GenericPackager ("src/config/packager/iso87ascii.xml"),
            "ISO87-Field64", "ISO87A-Field64");
    }
    @org.junit.jupiter.api.Test
    public void testXMLPackager () throws Exception {
        doTest (xmlPackager, "XMLPackager", "XMLPackager");
    }
    private void doTest (ISOPackager packager, String msg, String img)
        throws Exception
    {
        // Logger logger = new Logger();
        // logger.addListener (new SimpleLogListener (System.out));
        // packager.setLogger (logger, msg + "-m");

        ISOMsg m = getMsg (msg);
        m.setPackager (packager);
        byte[] p = m.pack();
        ByteArrayOutputStream out = new ByteArrayOutputStream ();
        m.pack (out);

        assertTrue (Arrays.equals (out.toByteArray(), p));

        writeImage (img, p);

        byte[] b = getImage (img);
        TestUtils.assertEquals(b, p);

        ISOMsg m1 = new ISOMsg ();
        // packager.setLogger (logger, msg + "-m1");
        m1.setPackager (packager);
        m1.unpack (b);
        TestUtils.assertEquals(b, m1.pack());

        ISOMsg m2 = new ISOMsg ();
        m2.setPackager (packager);
        // packager.setLogger (logger, msg + "-m2");
        m2.unpack (new ByteArrayInputStream (out.toByteArray()));
        TestUtils.assertEquals(b, m2.pack());
    }
}

