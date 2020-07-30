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

package com.futeh.posng.encoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface Encoder<T> {
    Ascii ASCII = new Ascii();
    Ebcdic EBCDIC = new Ebcdic();
    Bcd BCD_PADDED = new Bcd().pad(true); // left pad 0x00 if odd.
    Bcd BCD = new Bcd().pad(false);
    Binary BINARY = new Binary();

    void encode(OutputStream out, T value) throws IOException;
    T decode(InputStream in, int length) throws IOException;

    static byte[] read(InputStream in, int len) throws IOException {
        byte[] bytes = new byte [len];
        int read = 0;
        while (read < len) {
            int count = in.read(bytes, read, len - read);
            if (count < 0)
                throw new IOException("Reading error, expecting " + len + " bytes but only received " + read + " bytes.");
            read += count;
        }
        return bytes;
    }
}
