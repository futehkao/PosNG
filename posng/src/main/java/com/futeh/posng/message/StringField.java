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
import com.futeh.posng.length.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class StringField extends Field<String, StringField> {
    private char padWith = ' ';

    public StringField() {
        encoder(Encoder.ASCII);
    }

    public char getPadWith() {
        return padWith;
    }

    public void setPadWith(char padWith) {
        this.padWith = padWith;
    }

    public char padWith() {
        return padWith;
    }

    public StringField padWith(char padWith) {
        this.padWith = padWith;
        return this;
    }

    public StringField leftPadded(char padWith) {
        padding(Padding.LEFT);
        this.padWith = padWith;
        return this;
    }

    public StringField rightPadded(char padWith) {
        padding(Padding.RIGHT);
        this.padWith = padWith;
        return this;
    }

    public StringField ascii() {
        return encoder(Encoder.ASCII);
    }

    public StringField ebcdic() {
        return encoder(Encoder.EBCDIC);
    }

    // left padded
    public StringField bcdLeft() {
        return encoder(Encoder.BCD_PADDED);
    }

    // right padded
    public StringField bcdRight() {
        return encoder(Encoder.BCD);
    }

    public void setDataLength(DataLength dataLength) {
        super.setDataLength(dataLength);
        if (dataLength instanceof VarLen) {
            setPadding(Padding.NONE);
        }
    }

    public String defaultValue() {
        return "";
    }

    @Override
    public String read(InputStream in) throws IOException {
        int len = dataLength().read(in, maxLength());
        String value = encoder().decode(in, len);
        return pad(value, maxLength());
    }

    @Override
    public void write(OutputStream out, String value) throws IOException {
        if (value.length() > maxLength()) {
            throw new MessageException("Value too long for field=" + index() + " length=" + maxLength() + " value=" + value);
        }
        if (value.length() != maxLength() && padding() == Padding.NONE && dataLength() instanceof FixedLen) {
            throw new MessageException("Value length not matching for field=" + index() + " length=" + maxLength() + " value=" + value);
        }
        String padded = pad(value, maxLength());
        dataLength().write(out, padded.length());
        encoder().encode(out, padded);
    }

    protected String pad(String value, int length) {
        switch (getPadding()) {
            case NONE: return noPadding(value, length);
            case LEFT: return padLeft(value, length);
            case RIGHT: return padRight(value, length);
        }
        return noPadding(value, length);
    }

    protected String padLeft(String value, int length) {
        StringBuilder builder = new StringBuilder(length);
        if (value.length() == length)
            return value;
        for (int i = 0; i < length - value.length(); i++) {
            builder.append(padWith);
        }
        builder.append(value);
        return builder.toString();
    }

    public String padRight(String value, int length) {
        StringBuilder builder = new StringBuilder(length);
        if (value.length() == length)
            return value;
        builder.append(value);
        for (int i = 0; i < length - value.length(); i++) {
            builder.append(padWith);
        }
        return builder.toString();
    }

    public String noPadding(String value, int length) {
        return value;
    }
}
