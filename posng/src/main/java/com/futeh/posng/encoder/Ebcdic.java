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

package com.futeh.posng.encoder;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * What Character Sets does Mastercard Support?
 * Mastercard supports the following code pages for both standard and extended character sets:
 * • ASCII (ISO 8859-1)
 * • EBCDIC (Code Page 1047)
 */
public class Ebcdic implements Encoder<String> {
    public static final Charset CHARSET = Charset.forName("Cp1047");

    private Charset charset = CHARSET;

    public Charset getCharset() {
        return charset;
    }

    public void setCharset(Charset charset) {
        this.charset = charset;
    }

    public void encode(OutputStream out, String value) throws IOException {
        byte[] bytes = value.getBytes(charset);
        out.write(bytes);
    }

    public String decode(InputStream in, int length) throws IOException {
        byte[] bytes = Encoder.read(in, length);
        return new String(bytes, charset);
    }
}
