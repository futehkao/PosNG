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

import java.io.IOException;

public interface MessageHandler {
    default void handle(Connector connector) {
        Payload payload = null;
        try {
            payload = connector.read();
        } catch (IOException e) {
            connector.disconnect();
            if (!connector.reconnect())
                return;
        } catch(Exception ex) {
            connector.getEndpoint().onException(connector, payload, ex);
        }

        Payload p2 = payload;
        connector.execute(() -> {
            try {
                handle(connector, p2.getMessage());
            } catch (Exception ex) {
                connector.getEndpoint().onException(connector, p2.getMessage(), ex);
            }
        });
    }

    default Message afterReceived(Connector connector, Payload payload, Message message) {
        return message;
    }

    default Message beforeSend(Connector connector, Message message) {
        return message;
    }

    void handle(Connector connector, Message message) throws Exception;
}
