
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

import javax.net.ssl.SSLContext;

public class ConnectionInfo {
    private String host;
    private int port = -1;
    private boolean keepAlive = false;
    private int readTimeout = 0; // disabled
    private int soLinger = 10;
    private SSLContext sslContext;

    // server only
    private int backlog = 0;
    private String bindAddress = null;

    // client
    private int connectionTimeout = 10000; // 10 seconds
    private long reconnectInterval = 30000L;

    public ConnectionInfo host(String host) {
        this.host = host;
        return this;
    }

    public ConnectionInfo port(int port) {
        this.port = port;
        return this;
    }

    public ConnectionInfo keepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
        return this;
    }

    public ConnectionInfo readTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
        return this;
    }

    public ConnectionInfo soLinger(int soLinger) {
        this.soLinger = soLinger;
        return this;
    }

    public ConnectionInfo sslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
        return this;
    }

    public ConnectionInfo backLog(int backlog) {
        this.backlog = backlog;
        return this;
    }

    public ConnectionInfo bindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
        return this;
    }

    public ConnectionInfo connectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
        return this;
    }

    public ConnectionInfo reconnectInterval(long reconnectInterval) {
        this.reconnectInterval = reconnectInterval;
        return this;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(boolean keepAlive) {
        this.keepAlive = keepAlive;
    }

    public int getReadTimeout() {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout) {
        this.readTimeout = readTimeout;
    }

    public int getSoLinger() {
        return soLinger;
    }

    public void setSoLinger(int soLinger) {
        this.soLinger = soLinger;
    }

    public SSLContext getSslContext() {
        return sslContext;
    }

    public void setSslContext(SSLContext sslContext) {
        this.sslContext = sslContext;
    }

    public int getBacklog() {
        return backlog;
    }

    public void setBacklog(int backlog) {
        this.backlog = backlog;
    }

    public String getBindAddress() {
        return bindAddress;
    }

    public void setBindAddress(String bindAddress) {
        this.bindAddress = bindAddress;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public long getReconnectInterval() {
        return reconnectInterval;
    }

    public void setReconnectInterval(long reconnectInterval) {
        this.reconnectInterval = reconnectInterval;
    }
}
