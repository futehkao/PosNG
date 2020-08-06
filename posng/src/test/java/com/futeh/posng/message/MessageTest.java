/*
 * Copyright 2015-2020 Futeh Kao
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.futeh.posng.message;

import com.futeh.posng.length.VarLen;
import com.futeh.progeny.iso.*;
import com.futeh.progeny.iso.packager.GenericPackager;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.futeh.posng.DataElements.*;
import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    Composite create() {
        return new Composite()
                .set(1, new BitMapField(16))
                .set(2, a_char(3))
                .set(3, new Composite()
                        .set(1, a_char(2))
                        .set(2, a_char(2)))
                .set(65, new BitMapField(8))
                .set(66, a_char(3))
                .set(129, a_char(3));
    }

    @Test
    void basic() throws IOException {
        Composite composite = create();
        Message msg = new Message()
                .set(2, "123")
                .set(3, new Message()
                        .set(1, "ab")
                        .set(2, "cd"))
                .set(66, "456")
                .set(129, "789");

        // test read and write
        byte[] bytes = composite.write( msg);
        Message msg2 = composite.read(bytes);
        assertEquals((Object)msg.get(2), msg2.get(2));
        assertEquals((Object)msg.get(129), msg2.get(129));

        // get get
        assertEquals(msg.get("3.1"), "ab");
    }

    /**
     * test no fields being set.  We should still get a bitmap.
     * @throws Exception
     */
    @Test void empty() throws Exception {
        Composite composite = create();
        Message msg = new Message();
        byte[] bytes = composite.write( msg);
        assertTrue(bytes.length > 0);
        InputStream in = new ByteArrayInputStream(bytes);
        Message msg2 = composite.read(bytes);
    }

    @Test void varLen() throws IOException {
        Composite composite = new Composite()
                .set(1, new BitMapField(16))
                .set(2, a_char(99).noPadding().dataLength(new VarLen().digits(2).bcd()))
                .set(3, new Composite()
                        .dataLength(BB)
                        .set(1, a_char(2))
                        .set(2, a_char(2)));
        Message msg = new Message()
                .set(2, "12345678910")
                .set(3, new Message()
                        .set(1, "ab")
                        .set(2, "cd"));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        composite.write(out, msg);
        byte[] bytes = out.toByteArray();
        InputStream in = new ByteArrayInputStream(bytes);
        Message msg2 = composite.read(in);
        assertEquals((Object)msg.get(2), msg2.get(2));
    }

    @Test
    void progency() throws Exception {
        GenericPackager packager = new GenericPackager();
        ISOFieldPackager[] packagers = new ISOFieldPackager[]{
                new IFE_CHAR(4, "MESSAGE TYPE INDICATOR"),
                new IFB_BITMAP(16, "BIT MAP"),
                new IFE_CHAR(4, "Field 2"),
                new IFE_CHAR(4, "Field 3")
        };

        packager.setFieldPackager(packagers);
        ISOMsg msg = new ISOMsg();
        msg.setPackager(packager);
        msg.setMTI("0100");
        msg.set(2, "0123");
        msg.set(3, "1234");
        byte[] bytes = msg.pack();

        Composite msg2 = new Composite();
        msg2.set(0, ebcdic(4, F))
                .set(1, new BitMapField(16))
                .set(2, ebcdic(4, F))
                .set(3, ebcdic(4, F));

        Message map = msg2.read(bytes);
        assertEquals(msg.getValue(2), map.get(2));
        assertEquals(msg.getValue(3), map.get(3));

        bytes = msg2.write(map);

        msg = new ISOMsg();
        msg.setPackager(packager);
        msg.unpack(bytes);

        assertEquals(msg.getValue(2), map.get(2));
        assertEquals(msg.getValue(3), map.get(3));
    }
}
