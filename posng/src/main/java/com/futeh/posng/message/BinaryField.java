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

public class BinaryField extends Field<byte[], BinaryField> {

    public BinaryField() {
        encoder(Encoder.BINARY);
    }

    @Override
    public void write(OutputStream out, byte[] value) throws IOException {
        if (value.length > maxLength()) {
            throw new MessageException("Value too long for field=" + index() + " length=" + maxLength() + " value=" + value);
        }
        dataLength().write(out, value.length);
        encode(out, value);
    }

    @Override
    public byte[] read(InputStream in) throws IOException {
        int len = dataLength().read(in, maxLength());
        return decode(in, len);
    }

    protected void encode(OutputStream out, byte[] value) throws IOException {
        out.write(value);
    }

    protected byte[] decode(InputStream in, int length) throws IOException {
        return Encoder.read(in, length);
    }
}
