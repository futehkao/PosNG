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

import com.futeh.posng.message.MessageException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

@SuppressWarnings("unchecked")
public class VarLen implements DataLength {

    private int digits;
    private LengthEncoder lengthEncoder = LengthEncoder.ASCII;

    public VarLen() {
    }

    public VarLen(int numDigits) {
        this.digits = numDigits;
    }

    @Override
    public void validate(int maxLength) {
        if (digits <=0 )
            throw new MessageException("Digits not set.");
        int num = 1;
        for (int i = 0; i < digits; i++)
            num *= 10;
        if (maxLength >= num)
            throw new MessageException("maxLength " + maxLength + " exceeds the capacity of the number of digits " + digits);
    }

    public int getDigits() {
        return digits;
    }

    public void setDigits(int numDigits) {
        this.digits = numDigits;
    }

    public int digits() {
        return digits;
    }

    public VarLen digits(int numDigits) {
        this.digits = numDigits;
        return this;
    }

    public LengthEncoder getLengthEncoder() {
        return lengthEncoder;
    }

    public void setLengthEncoder(LengthEncoder encoder) {
        this.lengthEncoder = encoder;
    }

    public LengthEncoder lengthEncoder() {
        return lengthEncoder;
    }

    public VarLen lengthEncoder(LengthEncoder encoding) {
        this.lengthEncoder = encoding;
        return  this;
    }

    public VarLen ascii() {
        this.lengthEncoder = LengthEncoder.ASCII;
        return  this;
    }

    public VarLen ebcdic() {
        this.lengthEncoder = LengthEncoder.EBCDIC;
        return this;
    }

    public VarLen bcd() {
        this.lengthEncoder = LengthEncoder.BCD;
        return this;
    }

    public VarLen binary() {
        this.lengthEncoder = LengthEncoder.BINARY;
        return this;
    }

    public int read(InputStream in) throws IOException {
        return lengthEncoder.read(in, digits);
    }

    @Override
    public int read(InputStream in, int maxLength) throws IOException {
        return lengthEncoder.read(in, digits);
    }

    @Override
    public void write(OutputStream out, int length) throws IOException {
        lengthEncoder.write(out, digits , length);
    }

}
