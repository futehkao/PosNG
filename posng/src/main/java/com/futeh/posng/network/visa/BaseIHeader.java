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

package com.futeh.posng.network.visa;


import com.futeh.posng.encoder.Binary;
import com.futeh.posng.encoder.Encoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

public class BaseIHeader extends Binary {

    @Override
    public void encode(OutputStream out, byte[] value) throws IOException {
        // dest is field 5 from byte 5 to byte 7, src is field 6, from byte 8 to 11
        byte[] dst = Arrays.copyOfRange(value, 5, 8);
        byte[] src = Arrays.copyOfRange(value, 8, 12);
        byte[] flipped = Arrays.copyOf(value, value.length);
        System.arraycopy(src, 0, flipped, 5, 3);
        System.arraycopy(dst, 0, flipped, 8, 3);

        out.write(flipped);
    }

    @Override
    public byte[] decode(InputStream in, int length) throws IOException {
        int hdrLen = in.read();
        byte[] hdr = Encoder.read(in, hdrLen - 1);
        int hdr2Len = 0;
        byte[] hdr2 = null;
        if ((hdr[0] & 0x80) != 0) {
            // based on Visa doc, additional header may be present for VisaNet. Chapter 2.7 in Tech Spec Volume 1.
            hdr2Len = in.read();
            hdr2 = Encoder.read(in, hdr2Len - 1);;
        }
        byte[] bytes = new byte[hdrLen + hdr2Len];
        bytes[0] = (byte) hdrLen;
        System.arraycopy(hdr, 0, bytes, 1, hdrLen - 1);
        if (hdr2Len > 0) {
            bytes[hdrLen] = (byte) hdr2Len;
            System.arraycopy(hdr2, 0, bytes, hdrLen + 1, hdr2Len - 1);
        }
        return bytes;
    }
}
