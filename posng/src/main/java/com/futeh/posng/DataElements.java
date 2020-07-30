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

import com.futeh.posng.length.DataLength;
import com.futeh.posng.length.FixedLen;
import com.futeh.posng.length.VarLen;
import com.futeh.posng.message.BinaryField;
import com.futeh.posng.message.StringField;

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
    public static DataLength HH = new VarLen(2).binary();  // for Visa, it is called Hex and therefore H.
    public static DataLength HHH = new VarLen(3).binary();
    public static DataLength HHHH = new VarLen(4).binary();

    public static StringField string(int maxLength) {
        return new StringField().maxLength(maxLength);
    }

    public static StringField ascii(int maxLength) {
        return string(maxLength).ascii();
    }

    public static StringField ascii(int maxLength, DataLength length) {
        return string(maxLength).ascii().dataLength(length);
    }

    public static StringField ebcdic(int maxLength) {
        return string(maxLength).ebcdic();
    }

    public static StringField ebcdic(int maxLength, DataLength length) {
        return string(maxLength).ebcdic().dataLength(length);
    }

    public static StringField bcdPadded(int maxLength) {
        return string(maxLength).bcdLeft();
    }

    public static StringField bcdPadded(int maxLength, DataLength length) {
        return string(maxLength).bcdLeft().dataLength(length);
    }

    public static StringField bcd(int maxLength) {
        return string(maxLength).bcdRight();
    }

    public static StringField bcd(int maxLength, DataLength length) {
        return string(maxLength).bcdRight().dataLength(length);
    }

    public static BinaryField binary(int maxLength) {
        return new BinaryField().maxLength(maxLength);
    }

    public static BinaryField binary(int maxLength, DataLength length) {
        return new BinaryField().maxLength(maxLength).dataLength(length);
    }

    public static StringField CHAR(int maxLength) {
        return ascii(maxLength).dataLength(F).rightPadding().padChar(' ');
    }

    public static StringField CHAR(int maxLength, DataLength len) {
        return ascii(maxLength).dataLength(len).rightPadding().padChar(' ');
    }

    public static StringField ECHAR(int maxLength) {
        return ascii(maxLength).dataLength(F).rightPadding().padChar(' ');
    }

    public static StringField ECHAR(int maxLength, DataLength len) {
        return ebcdic(maxLength).dataLength(len).rightPadding().padChar(' ');
    }

    public static StringField NUM(int maxLength) {
        return bcd(maxLength).dataLength(F).noPadding();
    }

    public static StringField NUM(int maxLength, DataLength len) {
        return bcd(maxLength).dataLength(len).noPadding();
    }

    public static BinaryField BIN(int maxLength) {
        return binary(maxLength).dataLength(F);
    }

    public static BinaryField BIN(int maxLength, DataLength len) {
        return binary(maxLength).dataLength(len);
    }
}