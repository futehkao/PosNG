package com.futeh.posng.message.visa;

import com.futeh.posng.message.Message;
import com.futeh.progeny.iso.ISOException;
import com.futeh.progeny.iso.ISOMsg;
import com.futeh.progeny.iso.packager.GenericPackager;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

@Tag("posng")
public class Base1Test {

    private ISOMsg createMsg() throws ISOException {
        InputStream in = Base1.class.getResourceAsStream("base1.xml");
        GenericPackager packager = new GenericPackager(in);
        ISOMsg iso = new ISOMsg();
        iso.setPackager(packager);

        iso.setMTI("0100")
            .set(2, "0123456")
            .set(3, "1234")
            .set("44.1", "1")
            .set("44.2", "2")
            .set("44.3", "3")
            .set("44.4", "4")
            .set("44.5", "5")
            .set("44.6", "66")
            .set("44.7", "7")
            .set("44.8", "8")
            .set("44.9", "9")
            .set("44.10", "0")
            .set("44.11", "11")
            .set("44.12", "1")
            .set("44.13", "1")
            .set("44.14", "14")
            .set("44.15", "15")
            .set("44.16", "1");
        return iso;
    }


    @Test
    void basic() throws Exception {
        ISOMsg iso = createMsg();
        byte[] bytes = iso.pack();

        Message msg = new Base1().read(bytes);
        assertNotNull(msg.getComposite());
        assertEquals(msg.get("44.15").toString().trim(), iso.getString("44.15"));

        msg.set("44.15", "15"); // should be padded during write
        bytes = new Base1().write(msg);
        iso.getPackager().unpack(iso, bytes);

    }

    @Test
    void subfields() throws Exception {
        ISOMsg iso = createMsg();

        iso.setMTI("0100");
        iso.set(2, "0123456");
        iso.set(3, "1234");
        iso.set("62.1", "1");
        byte[] bytes = iso.pack();

        Message msg = new Base1().read(bytes);
        assertEquals(msg.get("62.1").toString(), iso.getString("62.1"));

        assertThrows(IllegalArgumentException.class, () -> msg.set("62.20", "x"));
        assertThrows(IllegalArgumentException.class, () -> msg.set("4.1", "x"));
    }
}
