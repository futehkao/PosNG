
/*
 * Copyright 2015-2020 Futeh Kao
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.*;
import com.futeh.posng.message.BitMap;
import com.futeh.posng.message.Message;
import com.futeh.posng.message.MessageException;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

@SuppressWarnings("unchecked")
public class JsonWriter {

    private ClassLoader loader = getClass().getClassLoader();
    private Map<Class, Deserializer> deserializers = new HashMap<>();
    private Map<String, Deserializer> deserializerAlias = new HashMap<>();

    public JsonWriter() {
        Class cls = Deserializer.class;
        while (cls != null && !cls.equals(Object.class)) {
            Field[] fields = cls.getDeclaredFields();
            for (Field f : fields) {
                try {
                    addSerializer(f);
                } catch (IllegalAccessException e) {
                    throw new MessageException(e);
                }
            }
            cls = cls.getSuperclass();
        }
    }

    private void addSerializer(Field f) throws IllegalAccessException {
        if (Modifier.isPublic(f.getModifiers())
                && Modifier.isStatic(f.getModifiers())) {
            Object from = f.get(null);
            if (from != null && from instanceof Deserializer) {
                Deserializer serializer = (Deserializer) from;
                deserializers.put(serializer.type(), serializer);
                deserializerAlias.put(serializer.alias(), serializer);
            }
        }
    }

    public ClassLoader getLoader() {
        return loader;
    }

    public void setLoader(ClassLoader loader) {
        this.loader = loader;
    }

    public static class BitSetSerializer extends com.fasterxml.jackson.databind.JsonSerializer<BitSet> {

        @Override
        public void serialize(BitSet value, JsonGenerator gen, SerializerProvider serializers)
                throws IOException {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put(Deserializer.CLASS, BitSet.class.getName());
            List<Integer> list = new ArrayList<>();
            for (int i = 0; i < value.length(); i++) {
                if (value.get(i))
                    list.add(i);
            }
            map.put("bitset", list);
            gen.writeObject(map);
        }

        @Override
        public Class<BitSet> handledType() {
            return BitSet.class;
        }
    }

    public static class BitSetDeserializer extends JsonDeserializer<BitSet> {

        @Override
        public BitSet deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException {
            BitSet ret = new BitSet();
            Map<String, Object> map = Deserializer.objectMapper.readValue(jsonParser, Map.class);
            Object obj = map.get("bitset");
            if (obj instanceof List) {
                List<Object> list = (List) obj;
                for (Object i : list) {
                    if (i instanceof Number)
                        ret.set(((Number) i).intValue());
                }
            }
            return ret;
        }
    }

    public String write(Message msg) throws JsonProcessingException {
        return write(msg, false);
    }

    public String write(Message msg, boolean withBitMap)  throws JsonProcessingException {
        if (withBitMap)
            return Deserializer.objectMapper.writeValueAsString(msg);
        else
            return Deserializer.objectMapper.writeValueAsString(removeBitMap(msg));
    }

    private Message removeBitMap(Message message) {
        Message clone = new Message();
        clone.setHeader(message.getHeader());
        for (Map.Entry<Integer, Object> entry : message.getContents().entrySet()) {
            if (entry.getValue() instanceof Message) {
                clone.set(entry.getKey(), removeBitMap((Message)entry.getValue()));
            } else if (!(entry.getValue() instanceof BitMap)) {
                clone.set(entry.getKey(), entry.getValue());
            }
        }
        return clone;
    }

    public Message read(String value) throws JsonProcessingException {
        Message msg = Deserializer.objectMapper.readValue(value, Message.class);
        deserialize(msg);
        return msg;
    }

    private void deserialize(Message msg) {
        for (Map.Entry<?, Object> entry : msg.getContents().entrySet()) {
            if (entry.getValue() instanceof Map) {
                Map m = (Map) entry.getValue();
                if (m.get(Deserializer.CLASS) != null) {
                    Object value = deserialize(m.get(Deserializer.CLASS).toString(), m);
                    if (value != null)
                        entry.setValue(value);
                }
            } else if (entry.getValue() instanceof Message) {
                deserialize((Message) entry.getValue());
            }
        }
    }

    private Object deserialize(String className, Map<?, Object> map) {
        try {
            Deserializer des = deserializerAlias.get(className);
            Class cls;
            if (des != null)
                cls = des.type();
            else {
                cls = loader.loadClass(className);
                des = deserializers.get(cls);
            }
            Object instance = cls.getDeclaredConstructor().newInstance();

            if (des != null)
                return des.deserialize(instance, map);
        } catch (Exception e) {
            throw new MessageException(e);
        }
        return null;
    }
}
