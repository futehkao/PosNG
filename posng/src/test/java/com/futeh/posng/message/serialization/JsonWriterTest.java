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

import com.futeh.posng.message.BitMapField;
import com.futeh.posng.message.Composite;
import com.futeh.posng.message.Message;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

@Tag("posng")
public class JsonWriterTest {
    @Test
    void basic() throws Exception {
        Composite composite = new Composite()
                .set(1, new BitMapField(16))
                .set(65, new BitMapField(8));
        Message msg = new Message()
                .set(2, "123")
                .set(3, new Message()
                        .set(1, "ab")
                        .set(2, "cd"))
                .set(66, "456")
                .set(129, "789");
        composite.createBitMap(msg);
        JsonWriter json = new JsonWriter();
        String str = json.write(msg);
        System.out.println(str);
        Message s = json.read(str);
    }
}
