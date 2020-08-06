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
import com.futeh.posng.length.FixedLen;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class BinaryField extends Field<byte[], BinaryField> {
    private static final byte[] DEFAULT_VAUE = new byte[0];

    private byte padWith = 0x00;

    public BinaryField() {
        encoder(Encoder.BINARY);
    }

    public byte getPadWith() {
        return padWith;
    }

    public void setPadWith(byte padWith) {
        this.padWith = padWith;
    }

    public byte padWith() {
        return padWith;
    }

    public BinaryField padWith(byte padWith) {
        this.padWith = padWith;
        return this;
    }

    public BinaryField leftPadded(byte padWith) {
        padding(Padding.LEFT);
        this.padWith = padWith;
        return this;
    }

    public BinaryField rightPadded(byte padWith) {
        padding(Padding.RIGHT);
        this.padWith = padWith;
        return this;
    }

    @Override
    public byte[] defaultValue() {
        return DEFAULT_VAUE;
    }

    @Override
    public void write(OutputStream out, byte[] value) throws IOException {
        if (value.length > maxLength()) {
            throw new MessageException("Value too long for field=" + index() + " length=" + maxLength() + " value=" + value);
        }
        if (value.length != maxLength() && padding() == Padding.NONE && dataLength() instanceof FixedLen) {
            throw new MessageException("Value length not matching for field=" + index() + " length=" + maxLength() + " value=" + value);
        }
        value = pad(value, maxLength());
        dataLength().write(out, value.length);
        encode(out, value);
    }

    @Override
    public byte[] read(InputStream in) throws IOException {
        int len = dataLength().read(in, maxLength());
        byte[] value = decode(in, len);
        return pad(value, maxLength());
    }

    protected void encode(OutputStream out, byte[] value) throws IOException {
        out.write(value);
    }

    protected byte[] decode(InputStream in, int length) throws IOException {
        return Encoder.read(in, length);
    }

    protected byte[] pad(byte[] value, int length) {
        switch (getPadding()) {
            case NONE: return noPadding(value, length);
            case LEFT: return padLeft(value, length);
            case RIGHT: return padRight(value, length);
        }
        return noPadding(value, length);
    }

    protected byte[] padLeft(byte[] value, int length) {
        if (value.length >= length)
            return value;
        byte[] padded = new byte[length];
        for (int i = 0; i < length - value.length; i++) {
            padded[i] = padWith;
        }
        System.arraycopy(value, 0, padded, length - value.length, value.length);
        return padded;
    }

    public byte[] padRight(byte[] value, int length) {
        if (value.length >= length)
            return value;
        byte[] padded = new byte[length];
        System.arraycopy(value, 0, padded, 0, value.length);
        for (int i = value.length; i < length; i++) {
           padded[i] = padWith;
        }
        return padded;
    }

    public byte[] noPadding(byte[] value, int length) {
        return value;
    }
}
