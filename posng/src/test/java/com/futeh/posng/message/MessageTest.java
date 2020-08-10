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
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static com.futeh.posng.DataElements.BB;
import static com.futeh.posng.DataElements.a_char;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Tag("posng")
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
        byte[] bytes = composite.write(msg);
        Message msg2 = composite.read(bytes);
        assertEquals((Object) msg.get(2), msg2.get(2));
        assertEquals((Object) msg.get(129), msg2.get(129));

        // get get
        assertEquals(msg.get("3.1"), "ab");
    }

    /**
     * test no fields being set.  We should still get a bitmap.
     *
     * @throws Exception
     */
    @Test
    void empty() throws Exception {
        Composite composite = create();
        Message msg = new Message();
        byte[] bytes = composite.write(msg);
        assertTrue(bytes.length > 0);
        InputStream in = new ByteArrayInputStream(bytes);
        Message msg2 = composite.read(bytes);
    }

    @Test
    void varLen() throws IOException {
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
        assertEquals((Object) msg.get(2), msg2.get(2));
    }
}
