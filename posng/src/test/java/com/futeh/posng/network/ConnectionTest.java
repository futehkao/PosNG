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

package com.futeh.posng.network;

import com.futeh.posng.message.BitMapField;
import com.futeh.posng.message.Composite;
import com.futeh.posng.message.Message;
import com.futeh.posng.network.visa.EAServer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.futeh.posng.DataElements.CHAR;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ConnectionTest {
    Composite composite;
    Message message;
    @BeforeEach
    void setup() {
        composite = new Composite()
                .component(1, new BitMapField(16))
                .component(2, CHAR(3))
                .component(3, new Composite()
                        .component(1, CHAR(2))
                        .component(2, CHAR(2)));

        message = new Message()
                .set(2, "123")
                .set(3, new Message()
                        .set(1, "ab")
                        .set(2, "cd"));
    }

    @Test
    void basic() throws Exception {
        AtomicBoolean clientShutdown = new AtomicBoolean(false);
        Endpoint server = new EAServer();
        server.setComposite(composite);
        server.setMessageHandler((connector, message) -> {
            System.out.println(message);
        });
        server.setConnectionHandler(connector -> {
            try {
                connector.write(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        server.setDisconnectionHandler(connector -> clientShutdown.set(true));
        server.getConnectionInfo().setPort(8583);
        server.runAsServer();
        Thread.sleep(1000L);

        Endpoint client = new EAServer();
        client.setComposite(composite);
        client.setMessageHandler((connector, message) -> {
            System.out.println(message);
        });
        client.getConnectionInfo().host("localhost").port(8583);
        client.runAsClient();

        synchronized (server) {
            server.wait(2000L);
        }

        client.shutdown();
        synchronized (server) {
            server.wait(1000L);
        }
        assertTrue(clientShutdown.get());

        server.shutdown();
        synchronized (server) {
            server.wait(2000L);
        }
    }
}

