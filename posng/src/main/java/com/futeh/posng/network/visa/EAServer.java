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

package com.futeh.posng.network.visa;

import com.futeh.posng.encoder.Encoder;
import com.futeh.posng.length.VarLen;
import com.futeh.posng.network.Connector;
import com.futeh.posng.network.Endpoint;
import com.futeh.posng.network.Payload;

import java.io.*;

public class EAServer extends Endpoint {

    private VarLen frame = new VarLen().binary().digits(2);

    @Override
    protected Payload readPayload(Connector connector, InputStream in) throws IOException {
        while (true) {
            int len = frame.read(in);
            byte[] bytes = Encoder.read(in, 2);

            if (len == 0) { // keepAlive
                connector.write(out -> writeBytes(out, new byte[4])); // send all zeros
            } else {
                if (bytes[1] == 0x00) { // data
                    Payload payload = new Payload(Encoder.read(in, len));
                    payload.setHeader(bytes);
                    return payload;
                } else if (bytes[1] == 0x23 || bytes[1] == 0x20) { // shutdown or heartbeat, need to look at the first two bytes in the payload
                    // shutdown: 0xE2C4, heartbeat: 0xC8C2
                }
            }
        }
    }

    @Override
    protected void writeBytes(OutputStream out, byte[] bytes) throws IOException {
        out.write(bytes.length >> 8);
        out.write(bytes.length);
        out.write(0);
        out.write(0);
        out.write(bytes);
    }
}
