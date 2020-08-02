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

/**
 * For an-x, see for example visa DE28
 * Typically, this is a fixed length
 * 1 AN, EBCDIC + 8 N, EBCDIC total: 9 bytes
 * for MasterCard the A and N can be ASCII.
 */
public class AmountField extends StringField {

    public AmountField() {
        padding(Padding.LEFT)
                .padChar('0')
                .dataLength(new FixedLen())
                .encoder(Encoder.EBCDIC)
                .maxLength(9);
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
        String sign = value.substring(0, 1); // usually a C or D
        String amount = value.substring(1);
        String padded = sign + pad(amount, maxLength() - 1);
        dataLength().write(out, padded.length());
        encoder().encode(out, padded);
    }
}
