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

package com.futeh.posng.message.serialization;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.futeh.posng.message.BitMap;
import com.futeh.posng.message.Message;
import com.futeh.posng.message.MessageException;

import java.util.*;

public interface Deserializer<T> extends SerializeFormat {
    ObjectMapper objectMapper = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    String CLASS = "class";
    Deserializer<BitMap> BITMAP = new BitMapDeserializer();
    Deserializer<Message> MESSAGE = new MessageDeserializer();

    default T deserialize(T instance, Map<String, Object> map) {
        try {
            return objectMapper.readValue(objectMapper.writeValueAsString(map), type());
        } catch (JsonProcessingException e) {
            throw new MessageException(e);
        }
    }

    @Override
    Class<T> type();

    class BitMapDeserializer implements Alias, Deserializer<BitMap> {
        @Override
        public Class<BitMap> type() {
            return BitMap.class;
        }
    }

    class MessageDeserializer implements Alias, Deserializer<Message> {

        @Override
        public Class<Message> type() {
            return Message.class;
        }
    }
}
