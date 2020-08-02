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
    public enum Padding {
        NONE,
        LEFT,
        RIGHT;
    }

    private char padChar = ' ';
    private Padding padding = Padding.NONE;

    public StringField() {
        encoder(Encoder.ASCII);
    }

    public char getPadChar() {
        return padChar;
    }

    public void setPadChar(char padChar) {
        this.padChar = padChar;
    }

    public char padChar() {
        return padChar;
    }

    public StringField padChar(char padChar) {
        this.padChar = padChar;
        return this;
    }

    public Padding getPadding() {
        return padding;
    }

    public void setPadding(Padding padding) {
        this.padding = padding;
    }

    public Padding padding() {
        return padding;
    }

    public StringField padding(Padding padding) {
        this.padding = padding;
        return this;
    }

    public StringField noPadding() {
        this.padding = Padding.NONE;
        return this;
    }

    public StringField leftPadded() {
        this.padding = Padding.LEFT;
        return this;
    }

    public StringField leftPadded(char padChar) {
        this.padding = Padding.LEFT;
        this.padChar = padChar;
        return this;
    }

    public StringField rightPadded() {
        this.padding = Padding.RIGHT;
        return this;
    }

    public StringField rightPadded(char padChar) {
        this.padding = Padding.RIGHT;
        this.padChar = padChar;
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

    @Override
    public String read(InputStream in) throws IOException {
        int len = dataLength().read(in, maxLength());
        return encoder().decode(in, len);
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
        switch (padding) {
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
            builder.append(padChar);
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
            builder.append(padChar);
        }
        return builder.toString();
    }

    public String noPadding(String value, int length) {
        return value;
    }
}
