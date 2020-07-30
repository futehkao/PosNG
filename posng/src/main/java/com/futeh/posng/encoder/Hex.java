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

import javax.xml.bind.DatatypeConverter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Hex implements Encoder<String> {
    @Override
    public void encode(OutputStream out, String value) throws IOException {
        String hexString = value.replaceAll("\\s","");
        byte[] bytes = DatatypeConverter.parseHexBinary(hexString);
        out.write(bytes);
    }

    @Override
    public String decode(InputStream in, int length) throws IOException {
        return DatatypeConverter.printHexBinary(Encoder.read(in, length)).toUpperCase();
    }
}
