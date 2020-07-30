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


import com.futeh.posng.encoder.*;
import com.futeh.posng.encoder.Encoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public interface LengthEncoder {
    LengthEncoder ASCII = new AsciiLength();
    LengthEncoder EBCDIC = new EbcdicLength();
    LengthEncoder BCD = new BcdLength();
    LengthEncoder BINARY = new BinaryLength();

    int read(InputStream in, int numDigits) throws IOException;
    void write(OutputStream out, int numDigits, int length) throws IOException;

    class AsciiLength implements LengthEncoder {

        @Override
        public int read(InputStream in, int numDigits) throws IOException {
            byte[] bytes = Encoder.read(in, numDigits);
            String str = new String(bytes, Ascii.CHARSET);
            return Integer.parseInt(str);
        }

        @Override
        public void write(OutputStream out, int numDigits, int length) throws IOException {
            byte[] bytes = Integer.toString(length).getBytes(Ascii.CHARSET);
            for (int i = 0; i < numDigits - bytes.length; i ++) {
                out.write('0');
            }

            for (int i = 0; i < bytes.length; i ++) {
                out.write(bytes[i]);
            }
        }
    }

    class EbcdicLength implements LengthEncoder {

        public int read(InputStream in, int numDigits) throws IOException {
            byte[] bytes = Encoder.read(in, numDigits);
            String str = new String(bytes, Ebcdic.CHARSET);
            return Integer.parseInt(str);
        }

        public void write(OutputStream out, int numDigits, int length) throws IOException {
            byte[] bytes = Integer.toString(length).getBytes(Ebcdic.CHARSET);
            for (int i = 0; i < numDigits - bytes.length; i ++) {
                out.write(0xF0);
            }

            for (int i = 0; i < bytes.length; i ++) {
                out.write(bytes[i]);
            }
        }
    }

    class BcdLength implements LengthEncoder {

        public int read(InputStream in, int width) throws IOException {
            return Integer.parseInt(Encoder.BCD_PADDED.decode(in, width));
        }

        public void write(OutputStream out, int numDigits, int length) throws IOException {
            String str = Integer.toString(length);
            StringBuilder builder = new StringBuilder(numDigits);
            while (builder.length() < numDigits - str.length()) {
                builder.append('0');
            }
            builder.append(str);

            Encoder.BCD_PADDED.encode(out, builder.toString());
        }
    }

    class BinaryLength implements LengthEncoder {

        public int read(InputStream in, int numDigits) throws IOException {
            byte[] bytes = Encoder.read(in, numDigits);
            int len = 0;
            for (int i = 0; i < numDigits; i++) {
                len <<= 8;
                len += bytes[i] & 0xff;
            }
            return len;
        }

        public void write(OutputStream out, int numDigits, int length) throws IOException {
            int len = length;
            // network order need to write high bytes first
            int mask = 0xff000000 >>> 8 * (4 - numDigits);
            for (int i = 0; i < numDigits; i++ ) {
                byte b = (byte) (len & mask);
                out.write(b);
                mask >>>= 8;
            }
        }
    }
}
