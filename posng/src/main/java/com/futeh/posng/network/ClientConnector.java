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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Socket;

public class ClientConnector extends Connector {
    private static final Logger logger = LoggerFactory.getLogger(ClientConnector.class);

    public void start() {
        reconnect();
    }

    protected void createSocket() throws IOException {
        String host = getConnectionInfo().getHost();
        int port = getConnectionInfo().getPort();
        if (getConnectionInfo().getSslContext() != null) {
            socket = getConnectionInfo().getSslContext().getSocketFactory()
                    .createSocket(host, port);
        } else {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), getConnectionInfo().getConnectionTimeout());
            } catch (IOException e) {
                socket = null;
                throw e;
            }
        }
        connect(socket);
    }

    public synchronized boolean reconnect() {
        if (socket != null)
            return true;

        while (!getEndpoint().isShutdown()) {
            try {
                createSocket();
                logger.info("connected to host:{} port:{}", getConnectionInfo().getHost(), getConnectionInfo().getPort());
                break;
            } catch (Exception ex) {
                logger.info("failed to connect to host: {} port:{}, {}", getConnectionInfo().getHost(), getConnectionInfo().getPort(), ex);
                try {
                    Thread.sleep(getConnectionInfo().getReconnectInterval());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        if (getEndpoint().isShutdown())
            disconnect();

        return !getEndpoint().isShutdown();
    }
}
