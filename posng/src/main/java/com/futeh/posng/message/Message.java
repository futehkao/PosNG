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

package com.futeh.posng.message;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.futeh.posng.message.serialization.Alias;
import com.futeh.posng.message.serialization.JsonWriter;

import java.util.*;

@JsonPropertyOrder("class")
public class Message implements Alias {
    private static JsonWriter jsonWriter = new JsonWriter();
    private byte[] header;
    private Map<String, Object> attributes;
    private SortedMap<Integer, Object> contents = new TreeMap<>();

    public byte[] getHeader() {
        return header;
    }

    public void setHeader(byte[] header) {
        this.header = header;
    }

    @JsonIgnore
    public Map<String, Object> getAttributes() {
        return attributes;
    }

    @JsonIgnore
    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Object getAttribute(String key) {
        if (attributes == null)
            return null;
        return attributes.get(key);
    }

    public Message setAttribute(String key, Object value) {
        if (attributes == null)
            attributes = new HashMap<>();
        attributes.put(key, value);
        return this;
    }

    public SortedMap<Integer, Object> getContents() {
        return contents;
    }

    public void setContents(SortedMap<Integer, Object> contents) {
        this.contents = contents;
    }

    public Object get(int index) {
        return contents.get(index);
    }

    public Object get(String index) {
        String[] tokens = index.split("\\.");
        if (tokens.length == 0)
            return null;
        SortedMap<Integer, Object> current = getContents();
        for (int i = 0; i < tokens.length - 1; i++) {
            String p = tokens[i];
            Object obj = current.get(Integer.parseInt(p.trim()));
            if (obj == null)
                return null;
            if (obj instanceof Message) {
                current = ((Message) obj).getContents();
            } else {
                throw new IllegalArgumentException("Invalid path " + index);
            }
        }
        return current.get(Integer.parseInt(tokens[tokens.length - 1].trim()));
    }

    public Message set(int index, Object value) {
        contents.put(index, value);
        return this;
    }

    public Object copy() {
        try {
            Message copy = new Message();
            if (attributes != null) {
                copy.attributes = new HashMap<>(attributes);
            }
            if (header != null)
                copy.header = Arrays.copyOf(header, header.length);
            copy.contents.putAll(contents);
            return copy;
        } catch (Exception e) {
            throw new MessageException(e);
        }
    }

    public Object copy(int[] fields) {
        try {
            Message copy = new Message();
            if (attributes != null) {
                copy.attributes = new HashMap<>(attributes);
            }
            if (header != null)
                copy.header = Arrays.copyOf(header, header.length);
            for (int field : fields) {
                copy.set(field, get(field));
            }
            return copy;
        } catch (Exception e) {
            throw new MessageException(e);
        }
    }

    public int lastKey() {
        if (contents.size() == 0)
            return -1;
        return contents.lastKey();
    }

    public String toString() {
        try {
            return jsonWriter.write(this);
        } catch (JsonProcessingException e) {
            return super.toString();
        }
    }
}
