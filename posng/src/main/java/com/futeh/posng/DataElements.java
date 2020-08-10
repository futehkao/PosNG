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

package com.futeh.posng;

import com.futeh.posng.encoder.Encoder;
import com.futeh.posng.length.DataLength;
import com.futeh.posng.length.FixedLen;
import com.futeh.posng.length.VarLen;
import com.futeh.posng.message.*;

public class DataElements {
    public static DataLength F = new FixedLen();
    public static DataLength AA = new VarLen(2).ascii();
    public static DataLength AAA = new VarLen(3).ascii();
    public static DataLength AAAA = new VarLen(4).ascii();
    public static DataLength AAAAA = new VarLen(5).ascii();
    public static DataLength EE = new VarLen(2).ebcdic();
    public static DataLength EEE = new VarLen(3).ebcdic();
    public static DataLength EEEE = new VarLen(4).ebcdic();
    public static DataLength EEEEE = new VarLen(5).ebcdic();
    public static DataLength BB = new VarLen(2).bcd();
    public static DataLength BBB = new VarLen(3).bcd();
    public static DataLength BBBB = new VarLen(4).bcd();
    public static DataLength BBBBB = new VarLen(5).bcd();
    // Usually AA, EE etc means the number of digits.  However for H, it means the number of bytes.
    public static DataLength H = new VarLen(1).binary();  // max is 255
    public static DataLength HH = new VarLen(2).binary(); // max is 2^16 - 1.

    public static Encoder ascii = Encoder.ASCII;
    public static Encoder ebcdic = Encoder.EBCDIC;
    public static Encoder bcd = Encoder.BCD;
    public static Encoder bcd_padded = Encoder.BCD_PADDED;
    public static Encoder binary = Encoder.BINARY;

    public static Composite composite() {
        return new Composite();
    }

    public static HeaderField header(int maxLength) {
        return new HeaderField(maxLength);
    }

    public static BitMapField bitmap(int maxLength) {
        return new BitMapField(maxLength);
    }

    public static StringField string(int maxLength) {
        return new StringField().maxLength(maxLength);
    }

    // ascii character right padded with spaces
    public static StringField a_char(int maxLength) {
        return string(maxLength).ascii().dataLength(F).rightPadded().padWith(' ');
    }

    public static StringField a_char(int maxLength, DataLength len) {
        return string(maxLength).ascii().dataLength(len).rightPadded().padWith(' ');
    }

    // ebcdic character right padded with spaces
    public static StringField e_char(int maxLength) {
        return string(maxLength).ebcdic().dataLength(F).rightPadded().padWith(' ');
    }

    public static StringField e_char(int maxLength, DataLength len) {
        return string(maxLength).ebcdic().dataLength(len).rightPadded().padWith(' ');
    }

    // BCD encoded number, left padded with 0
    public static StringField b_num(int maxLength) {
        return string(maxLength).bcdLeft().dataLength(F).leftPadded('0');
    }

    public static StringField b_num(int maxLength, DataLength len) {
        return string(maxLength).bcdLeft().dataLength(len).leftPadded('0');
    }

    public static StringField a_num(int maxLength) {
        return string(maxLength).ascii().dataLength(F).leftPadded('0');
    }

    public static StringField a_num(int maxLength, DataLength len) {
        return string(maxLength).ascii().dataLength(len).leftPadded('0');
    }

    public static StringField e_num(int maxLength) {
        return string(maxLength).ebcdic().dataLength(F).leftPadded('0');
    }

    public static StringField e_num(int maxLength, DataLength len) {
        return string(maxLength).ebcdic().dataLength(len).leftPadded('0');
    }

    // binary
    public static BinaryField bin(int maxLength) {
        return new BinaryField(maxLength);
    }

    public static BinaryField bin(int maxLength, DataLength len) {
        return new BinaryField(maxLength).dataLength(len);
    }

    public static AmountField e_amt(int maxLength) {
        return new AmountField();
    }

    public static AmountField a_amt(int maxLength) {
        return (AmountField) new AmountField().encoder(Encoder.ASCII);
    }
}
