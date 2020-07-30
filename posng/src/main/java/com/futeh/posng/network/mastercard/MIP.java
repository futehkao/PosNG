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

package com.futeh.posng.network.mastercard;

import com.futeh.posng.encoder.Encoder;
import com.futeh.posng.length.VarLen;
import com.futeh.posng.network.Connector;
import com.futeh.posng.network.Endpoint;
import com.futeh.posng.network.Payload;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class MIP extends Endpoint {
    private VarLen frame = new VarLen().binary().digits(2);

    @Override
    protected Payload readPayload(Connector connector, InputStream in) throws IOException {
        int len = frame.read(in);
        return new Payload(Encoder.read(in, len));

    }

    @Override
    protected void writeBytes(OutputStream out, byte[] bytes) throws IOException {
        out.write(bytes.length >> 8);
        out.write(bytes.length);
        out.write(bytes);
    }
}
