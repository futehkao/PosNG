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

// from https://gist.github.com/neuro-sys/953548/910ceec6c15d9286fb0672cd5bc304d86eb6ba67
public class Bcd implements Encoder<String> {
    boolean pad = true;

    public boolean pad() {
        return pad;
    }

    public Bcd pad(boolean pad) {
        this.pad = pad;
        return this;
    }

    public void encode(OutputStream out, String value) throws IOException {
        int length = value.length();
        int byteLen = length % 2 == 0 ? length / 2 : (length + 1) / 2;
        boolean isOdd = length % 2 != 0;
        for (int i = 0 ; i < byteLen; i++) {
            byte b = 0;
            int lo = 0;
            int hi = 0;
            if (pad && isOdd) {
                if (i == 0) {
                    lo = value.charAt(2 * i) - '0';
                    b |= lo;
                } else {
                    lo = value.charAt(2 * i) - '0';
                    hi = value.charAt(2 * i - 1) - '0';
                }
            } else {
                hi = value.charAt(2 * i) - '0';
                if (2 * i + 1 < value.length())
                    lo = value.charAt(2 * i + 1) - '0';
            }
            b |= lo;
            b |= hi << 4;
            out.write(b);
        }
    }

    public String decode(InputStream in, int length) throws IOException {
        int byteLen = length % 2 == 0 ? length / 2 : (length + 1) / 2;
        byte[] bytes = Encoder.read(in, byteLen);
        boolean odd = length % 2 != 0;
        StringBuilder builder = new StringBuilder();

        for (int i = 0 ; i < byteLen; i++) {
            if (pad && odd && i == 0) {
                builder.append(bytes[0] & 0x0f); // lo
            } else {
                builder.append((bytes[i] & 0xf0) >> 4); // hi
                if (builder.length() < length)
                    builder.append(bytes[i] & 0x0f); // lo
            }
        }
        return builder.toString();
    }
}
