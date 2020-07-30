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

import com.futeh.posng.message.Composite;
import com.futeh.posng.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;
import java.util.function.Consumer;

public abstract class Endpoint {
    private static final Logger logger = LoggerFactory.getLogger(Endpoint.class);
    protected Composite composite;
    private ExecutorService executorService;
    private int threadCoreSize = Runtime.getRuntime().availableProcessors();
    private int threadMaxSize = 100;
    private int threadKeepAliveSec = 10;
    private int threadQueueSize = 10;
    private String threadPoolName = "ISO8583 Pool";
    private MessageHandler messageHandler;
    private MessageExceptionHandler messageExceptionHandler;
    private PayloadExceptionHandler payloadExceptionHandler;
    private Consumer<Connector> connectionHandler;
    private Consumer<Connector> disconnectionHandler;
    private ConnectionInfo connectionInfo = new ConnectionInfo();
    private volatile boolean shutdown = true;
    private Runnable shutdownRunnable;
    private List<Connector> activeConnectors = Collections.synchronizedList(new LinkedList<>());

    public Composite getComposite() {
        return composite;
    }

    public void setComposite(Composite composite) {
        this.composite = composite;
    }

    public ExecutorService getExecutorService() {
        return executorService;
    }

    public void setExecutorService(ExecutorService executorService) {
        this.executorService = executorService;
    }

    public int getThreadCoreSize() {
        return threadCoreSize;
    }

    public void setThreadCoreSize(int threadCoreSize) {
        this.threadCoreSize = threadCoreSize;
    }

    public int getThreadMaxSize() {
        return threadMaxSize;
    }

    public void setThreadMaxSize(int threadMaxSize) {
        this.threadMaxSize = threadMaxSize;
    }

    public int getThreadKeepAliveSec() {
        return threadKeepAliveSec;
    }

    public void setThreadKeepAliveSec(int threadKeepAliveSec) {
        this.threadKeepAliveSec = threadKeepAliveSec;
    }

    public int getThreadQueueSize() {
        return threadQueueSize;
    }

    public void setThreadQueueSize(int threadQueueSize) {
        this.threadQueueSize = threadQueueSize;
    }

    public String getThreadPoolName() {
        return threadPoolName;
    }

    public void setThreadPoolName(String threadPoolName) {
        this.threadPoolName = threadPoolName;
    }

    public MessageHandler getMessageHandler() {
        return messageHandler;
    }

    public void setMessageHandler(MessageHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    public MessageExceptionHandler getMessageExceptionHandler() {
        return messageExceptionHandler;
    }

    public void setMessageExceptionHandler(MessageExceptionHandler messageExceptionHandler) {
        this.messageExceptionHandler = messageExceptionHandler;
    }

    public PayloadExceptionHandler getPayloadExceptionHandler() {
        return payloadExceptionHandler;
    }

    public void setPayloadExceptionHandler(PayloadExceptionHandler payloadExceptionHandler) {
        this.payloadExceptionHandler = payloadExceptionHandler;
    }

    public Consumer<Connector> getConnectionHandler() {
        return connectionHandler;
    }

    public void setConnectionHandler(Consumer<Connector> connectionHandler) {
        this.connectionHandler = connectionHandler;
    }

    public Consumer<Connector> getDisconnectionHandler() {
        return disconnectionHandler;
    }

    public void setDisconnectionHandler(Consumer<Connector> disconnectionHandler) {
        this.disconnectionHandler = disconnectionHandler;
    }

    public ConnectionInfo getConnectionInfo() {
        return connectionInfo;
    }

    public void setConnectionInfo(ConnectionInfo connectionInfo) {
        this.connectionInfo = connectionInfo;
    }

    public boolean isShutdown() {
        return shutdown;
    }

    public synchronized void shutdown() {
        shutdown = true;
        if (shutdownRunnable != null)
            shutdownRunnable.run();
        shutdownRunnable = null;
        activeConnectors.forEach(c -> c.disconnect());
    }

    public synchronized void runAsClient() {
        shutdown = false;
        ClientConnector connector = new ClientConnector();
        connector.setConnectionInfo(getConnectionInfo());
        runEventLoop(connector);
    }

    public synchronized void runAsServer() {
        shutdown = false;
        Server server = new Server();
        server.setConnectionInfo(getConnectionInfo());
        server.setEndpoint(this);
        CompletableFuture.runAsync(server);
    }

    public synchronized void runEventLoop(Connector connector) {
        if (messageHandler == null)
            throw new IllegalStateException("messageHandler not set.");

        shutdown = false;
        // start connector
        connector.setEndpoint(this);
        connector.start();

        // start thread pool
        if (executorService == null) {
            ThreadFactory factory = runnable -> {
                Thread thread = new Thread(runnable, threadPoolName);
                thread.setName(threadPoolName + "-" + thread.getId());
                thread.setDaemon(true);
                return thread;
            };

            executorService = new ThreadPoolExecutor(threadCoreSize, threadMaxSize, threadKeepAliveSec, TimeUnit.SECONDS,
                    new ArrayBlockingQueue<>(threadQueueSize), factory, new ThreadPoolExecutor.CallerRunsPolicy());

            Runtime.getRuntime().addShutdownHook(new Thread(this::stop));
        }
        startEventLoop(connector);
    }

    public synchronized void stop() {
        ExecutorService es = executorService;
        if (es != null) {
            es.shutdown();
        }
        executorService = null;
    }

    protected void startEventLoop(Connector connector) {
        if (executorService == null)
            throw new IllegalStateException("Call start first");
        new Thread(() -> {
            try {
                enterEventLoop(connector);
            } finally {
                connector.disconnect();
            }
        }).start();
    }

    protected void enterEventLoop(Connector connector) {
        try {
            activeConnectors.add(connector);
            while (!isShutdown()) {
                executorService.execute(() -> {
                    try {
                        messageHandler.handle(connector);
                    } catch (Exception ex) {
                        logger.warn("Unexpected exception in eventLoop.", ex);
                    }
                });
            }
        } finally {
            activeConnectors.remove(connector);
        }

    }

    public void execute(Runnable runnable) {
        if (executorService == null)
            throw new IllegalStateException("executorService not started");
        executorService.execute(runnable);
    }

    public void onException(Connector connector, Message message, Exception exception) {
        if (messageExceptionHandler != null)
            messageExceptionHandler.handle(connector, message, exception);
    }

    public void onException(Connector connector, Payload payload, Exception exception) {
        if (payloadExceptionHandler != null)
            payloadExceptionHandler.handle(connector, payload, exception);
    }

    public void onConnected(Connector connector) {
        if (connectionHandler != null)
            connectionHandler.accept(connector);
    }

    public void onDisconnected(Connector connector) {
        if (disconnectionHandler != null)
            disconnectionHandler.accept(connector);
    }

    protected abstract Payload readPayload(Connector connector, InputStream in) throws IOException;

    protected void received(Connector connector, Payload payload, Message msg) {
        // do nothing, expects subclass to change header etc.
    }

    protected void sending(Connector connector, Message msg) {
        // do nothing, expects subclass to change header etc.
    }

    protected abstract void writeBytes(OutputStream out, byte[] bytes) throws IOException;
}
