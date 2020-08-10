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

package com.futeh.posng.message.serialization;

import com.futeh.posng.message.Composite;
import com.futeh.posng.message.StringField;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("posng")
public class CompositeBuilderTest {

    String read(String file) throws IOException {
        InputStream in = CompositeBuilderTest.class.getResourceAsStream(file);
        StringBuilder builder = new StringBuilder();
        byte[] bytes = new byte[100];
        while(true) {
            int read = in.read(bytes);
            if (read < 0)
                break;
            builder.append(new String(bytes, 0, read, StandardCharsets.UTF_8));
        }
        in.close();
        return builder.toString();
    }

    @Test
    void config1() throws Exception {
        CompositeBuilder config = new CompositeBuilder();
        Composite composite = config.config(read("config1.json"))
                .set(10, "e_char, 12, EE")
                .getComposite();
    }

    @Test
    void config1_1() throws Exception {
        CompositeBuilder config = new CompositeBuilder();
        Composite composite = config.config(read("config1.json"))
                .set(10, "e_char, 12, EE")
                .getComposite();
    }

    @Test
    void config2() throws Exception {
        CompositeBuilder config = new CompositeBuilder();
        Composite msg = config.config(read("config2.json")).getComposite();
        StringField s = msg.get(2);
        assertEquals(s.maxLength(), 8);
        s = msg.get(3);
        assertEquals(s.maxLength(), 16);
    }
}
