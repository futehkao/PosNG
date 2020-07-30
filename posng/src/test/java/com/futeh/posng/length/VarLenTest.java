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

package com.futeh.posng.length;

import com.futeh.progeny.iso.AsciiPrefixer;
import com.futeh.progeny.iso.BcdPrefixer;
import com.futeh.progeny.iso.BinaryPrefixer;
import com.futeh.progeny.iso.EbcdicPrefixer;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import static com.futeh.posng.DataElements.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

class VarLenTest {

    @Test
    void ascii() throws Exception {
        AsciiPrefixer prefixer = AsciiPrefixer.LLLLL;
        byte[] bytes = new byte[5];
        prefixer.encodeLength(123, bytes);
        InputStream in = new ByteArrayInputStream(bytes);
        DataLength lvar = AAAAA;
        int len = lvar.read(in, 99999);
        assertEquals(len, 123);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        lvar.write(out, 123);
        bytes = out.toByteArray();
        len = prefixer.decodeLength(bytes, 0);
        assertEquals(len, 123);
    }

    @Test
    void ebcdic() throws Exception {
        EbcdicPrefixer prefixer = EbcdicPrefixer.LLLL;
        byte[] bytes = new byte[4];
        prefixer.encodeLength(123, bytes);
        InputStream in = new ByteArrayInputStream(bytes);
        DataLength lvar = EEEE;
        int len = lvar.read(in, 9999);
        assertEquals(len, 123);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        lvar.write(out, 123);
        bytes = out.toByteArray();
        len = prefixer.decodeLength(bytes, 0);
        assertEquals(len, 123);
    }

    @Test
    void bcd() throws Exception {
        BcdPrefixer prefixer = BcdPrefixer.LLLLL;
        byte[] bytes = new byte[3];
        prefixer.encodeLength(123, bytes);
        InputStream in = new ByteArrayInputStream(bytes);
        DataLength lvar = BBBBB;
        int len = lvar.read(in, 9999);
        assertEquals(len, 123);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        lvar.write(out, 123);
        bytes = out.toByteArray();
        len = prefixer.decodeLength(bytes, 0);
        assertEquals(len, 123);
    }

    @Test
    void binary() throws Exception {
        BinaryPrefixer prefixer = BinaryPrefixer.BB;
        byte[] bytes = new byte[2];
        prefixer.encodeLength(123, bytes);
        InputStream in = new ByteArrayInputStream(bytes);
        DataLength lvar = HH;
        int len = lvar.read(in, 9999);
        assertEquals(len, 123);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        lvar.write(out, 123);
        bytes = out.toByteArray();
        len = prefixer.decodeLength(bytes, 0);
        assertEquals(len, 123);
    }
}
