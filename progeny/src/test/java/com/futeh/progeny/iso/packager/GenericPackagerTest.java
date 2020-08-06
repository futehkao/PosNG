package com.futeh.progeny.iso.packager;

import com.futeh.progeny.iso.IFB_BITMAP;
import com.futeh.progeny.iso.IFE_CHAR;
import com.futeh.progeny.iso.ISOFieldPackager;
import com.futeh.progeny.iso.ISOMsg;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GenericPackagerTest {
    @Test
    void extended() throws Exception {
        GenericPackager packager = new GenericPackager();

        ISOFieldPackager[] packagers = new ISOFieldPackager[131];
        packagers[0] = new IFE_CHAR(4, "MESSAGE TYPE INDICATOR");
        packagers[1] = new IFB_BITMAP(16, "BIT MAP");
        packagers[2] = new IFE_CHAR(4, "Field 2");
        packagers[65] = new IFB_BITMAP(8, "BIT MAP");
        packagers[66] = new IFE_CHAR(4, "Field 66");
        packagers[129] = new IFE_CHAR(4, "Field 129");

        packager.setFieldPackager(packagers);
        packager.setExtendedBitmap(65);
        ISOMsg msg = new ISOMsg();
        msg.setPackager(packager);
        msg.setMTI("0100");
        msg.set(2, "0123");
        //
        msg.set(66, "1234");
        msg.set(129, "1234");
        byte[] bytes = msg.pack();
        ISOMsg msg2 = new ISOMsg();
        msg2.setPackager(packager);
        msg2.unpack(bytes);

        assertEquals(msg.getString(0), msg2.getString(0));
        assertEquals(msg.getString(66), msg2.getString(66));
        assertEquals(msg.getString(129), msg2.getString(129));
    }
}
