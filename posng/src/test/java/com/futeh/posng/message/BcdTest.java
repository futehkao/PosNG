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

import com.futeh.posng.encoder.Bcd;
import com.futeh.progeny.iso.BCDInterpreter;
import org.junit.jupiter.api.Test;

import java.io.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BcdTest {

    @Test
    void basic() throws IOException {
        String value = "12345";
        BCDInterpreter interpreter = BCDInterpreter.LEFT_PADDED;
        byte[] bytes = new byte[3];
        interpreter.interpret(value, bytes, 0);
        Bcd bcd = new Bcd();
        String val = bcd.decode(new ByteArrayInputStream(bytes), value.length());
        assertEquals(val, value);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bcd.encode(out, value);
        bytes = out.toByteArray();
        value = interpreter.uninterpret(bytes, 0, 5);
        val = bcd.decode(new ByteArrayInputStream(bytes), value.length());
        assertEquals(val, value);

        bcd.pad(false);
        interpreter = BCDInterpreter.RIGHT_PADDED;
        bytes = new byte[3];
        interpreter.interpret(value, bytes, 0);
        val = bcd.decode(new ByteArrayInputStream(bytes), value.length());
        assertEquals(val, value);

        out = new ByteArrayOutputStream();
        bcd.encode(out, value);
        bytes = out.toByteArray();
        value = interpreter.uninterpret(bytes, 0, 5);
        val = bcd.decode(new ByteArrayInputStream(bytes), value.length());
        assertEquals(val, value);
    }

    @Test
    void track2Like() throws IOException {
        String value = "12=45";
        Bcd bcd = new Bcd();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        bcd.encode(out, value);
        String value2 = bcd.decode(new ByteArrayInputStream(out.toByteArray()), 5);
        assertEquals(value, value2);
    }
}
