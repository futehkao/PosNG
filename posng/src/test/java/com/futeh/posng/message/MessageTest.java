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
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.futeh.posng.DataElements.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class MessageTest {

    @Test
    void basic() throws IOException {
        Composite composite = new Composite()
                .component(1, new BitMapField(16))
                .component(2, CHAR(3))
                .component(3, new Composite()
                        .component(1, CHAR(2))
                        .component(2, CHAR(2)))
                .component(65, new BitMapField(8))
                .component(66, CHAR(3))
                .component(129, CHAR(3));
        Message msg = new Message()
                .set(2, "123")
                .set(3, new Message()
                        .set(1, "ab")
                        .set(2, "cd"))
                .set(66, "456")
                .set(129, "789");

        // test read and write
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        composite.write(out, msg);
        byte[] bytes = out.toByteArray();
        InputStream in = new ByteArrayInputStream(bytes);
        Message msg2 = composite.read(in);
        assertEquals(msg.get(2), msg2.get(2));
        assertEquals(msg.get(129), msg2.get(129));

        // get get
        assertEquals(msg.get("3.1"), "ab");
    }

    @Test void varLen() throws IOException {
        Composite composite = new Composite()
                .component(1, new BitMapField(16))
                .component(2, CHAR(99).noPadding().dataLength(new VarLen().digits(2).bcd()))
                .component(3, new Composite()
                        .dataLength(BB)
                        .component(1, CHAR(2))
                        .component(2, CHAR(2)));
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
        assertEquals(msg.get(2), msg2.get(2));
    }

    @Test
    void jpos() throws Exception {
        ISOBasePackager packager = new ISOBasePackager() {
        };
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
        msg2.component(0, ebcdic(4, F))
                .component(1, new BitMapField(16))
                .component(2, ebcdic(4, F))
                .component(3, ebcdic(4, F));

        InputStream in = new ByteArrayInputStream(bytes);
        Message map = msg2.read(in);
        assertEquals(msg.getValue(2), map.get(2));
        assertEquals(msg.getValue(3), map.get(3));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        msg2.write(out, map);

        msg = new ISOMsg();
        msg.setPackager(packager);
        msg.unpack(out.toByteArray());

        assertEquals(msg.getValue(2), map.get(2));
        assertEquals(msg.getValue(3), map.get(3));
    }
}
