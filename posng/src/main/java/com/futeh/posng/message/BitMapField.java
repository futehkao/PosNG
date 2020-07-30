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

import com.futeh.posng.encoder.Encoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BitMapField extends Field<BitMap, BitMapField> {
    public BitMapField() {
    }

    public BitMapField(int maxLength) {
        maxLength(maxLength);
    }

    @Override
    public void write(OutputStream out, BitMap bitMap) throws IOException {
        int len;

        if (bitMap.length() > 129)
            len = 24;
        else if (bitMap.length() > 65)
            len = 16;
        else
            len = 8;

        writeBits(out, bitMap, len);
    }

    @Override
    public BitMap read(InputStream in) throws IOException {
        BitMap bitMap = setBits(new BitMap(65), Encoder.read(in, 8), 0);
        if (maxLength() > 8 && bitMap.get(1)) {
            setBits(bitMap, Encoder.read(in, 8), 64);
        }
        // tertiary bit map.
        if (maxLength() > 16 && bitMap.get(65)) {
            setBits(bitMap, Encoder.read(in, 8), 128);
        }
        return bitMap;
    }

    protected BitMap setBits(BitMap bitMap, byte[] bytes, int offset) {
        int base = 0;
        for (byte b : bytes) {
            for (int i = 0; i < 8; i++) {
                if ((b & (0x80 >> i)) > 0)
                    bitMap.set(base + i + offset + 1);
            }
            base += 8;
        }
        return bitMap;
    }

    protected void writeBits(OutputStream out, BitMap bitMap, int numBytes) throws IOException {
        int len = numBytes * 8;
        for (int i = 0; i < numBytes; i++) {
            byte b = 0;
            for (int j = 0; j < 8; j++) {
                if (bitMap.get(i * 8 + j + 1))
                    b |= 0x80 >> j;
            }
            if (len > 64 && i == 0)
                b |= 0x80;
            if (len > 128 && i == 8)
                b |= 0x80;
            out.write(b);
        }
    }
}
