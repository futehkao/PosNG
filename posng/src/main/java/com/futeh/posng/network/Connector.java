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

import com.futeh.posng.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.Socket;
import java.net.SocketException;

public abstract class Connector {
    private static final String CONNECTOR = "connector";
    private static final Logger logger = LoggerFactory.getLogger(Connector.class);

    protected Socket socket;
    private Object readLock = new Object();
    private Object writeLock = new Object();
    protected InputStream in;
    protected OutputStream out;
    private Endpoint endpoint;
    private ConnectionInfo connectionInfo = new ConnectionInfo();

    public ConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    public void setConnectionInfo(ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    public Socket getSocket() {
        return socket;
    }

    public void setSocket(Socket socket) {
        this.socket = socket;
    }

    public Endpoint getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(Endpoint endpoint) {
        this.endpoint = endpoint;
    }

    public <R> R read(FunctionWithException<InputStream, R, IOException> function) throws IOException {
        synchronized (readLock) {
            InputStream input = in;
            if (input == null)
                throw new IOException("Socket closed.");
            return function.apply(input);
        }
    }

    public void write(ConsumerWithException<OutputStream, IOException> consumer) throws IOException {
        synchronized (writeLock) {
            OutputStream output = out;
            if (output == null)
                throw new IOException("Socket closed.");
            consumer.accept(output);
        }
    }

    public abstract void start();

    protected void connect(Socket s) throws IOException {
        try {
            s.setKeepAlive(connectionInfo.isKeepAlive());
            if (connectionInfo.getReadTimeout() > 0)
                s.setSoTimeout(connectionInfo.getReadTimeout());
            in = new BufferedInputStream(s.getInputStream());
            out = new BufferedOutputStream(s.getOutputStream());
            endpoint.onConnected(this);
        } catch (IOException ex) {
            disconnect();
            throw ex;
        }
    }

    public synchronized void disconnect() {
        if (socket == null)
            return;

        try {
            if (connectionInfo.getSoLinger() > 0)
                socket.setSoLinger(true, connectionInfo.getSoLinger());
        } catch (SocketException ex) {
            logger.info("Error setting soLinger ", ex);
        }

        try {
            socket.close();
            in.close();
            out.close();
        } catch (IOException ex) {
            logger.info("Error closing socket", ex);
        }
        endpoint.onDisconnected(this);
        socket = null;
        in = null;
        out = null;
    }

    public abstract boolean reconnect();

    public Payload read() throws IOException {
        Payload payload = read(in -> endpoint.readPayload(this, in));
        InputStream bin = new ByteArrayInputStream(payload.getData(), payload.getOffset(), payload.getMessageLength());
        Message msg = endpoint.getComposite().read(bin);
        msg.setAttribute(CONNECTOR, this);
        msg = endpoint.getMessageHandler().afterReceived(this, payload, msg);
        msg.setAttribute(CONNECTOR, this);
        payload.setMessage(msg);
        return payload;
    }

    public void write(Message message) throws IOException {
        message.setAttribute(CONNECTOR, this);
        Message msg = endpoint.getMessageHandler().beforeSend(this, message);
        ByteArrayOutputStream tmp = new ByteArrayOutputStream();
        endpoint.getComposite().write(tmp, msg);
        byte[] bytes = tmp.toByteArray();
        write(out -> {
            endpoint.writeBytes(out, bytes);
            out.flush();
        });
    }

    public void execute(Runnable runnable) {
        endpoint.execute(runnable);
    }
}
