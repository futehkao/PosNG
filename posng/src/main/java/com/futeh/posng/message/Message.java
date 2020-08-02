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

import java.io.OutputStream;
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

    public Map<Integer, Object> getContents() {
        return contents;
    }

    public void setContents(Map<Integer, Object> contents) {
        this.contents = new TreeMap<>(contents);
    }

    public <T> T get(int index) {
        return(T) contents.get(index);
    }

    public <T> T get(String index) {
        String[] tokens = index.split("\\.");
        if (tokens.length == 0)
            return null;
        Map<Integer, Object> current = getContents();
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
        return (T) current.get(Integer.parseInt(tokens[tokens.length - 1].trim()));
    }

    public Message set(int index, Object value) {
        if (value == null)
            unset(index);
        else
            contents.put(index, value);
        return this;
    }

    public Message unset(int index) {
        contents.remove(index);
        return this;
    }

    // https://en.wikipedia.org/wiki/ISO_8583
    // Message Origin
    public Message copyResponse() {
        if (!contents.containsKey(0))
            throw new MessageException("MTI not present");

        String mti = contents.get(0).toString();
        int mtiNum = Integer.parseInt(mti);
        if ((mtiNum / 10) % 2 != 0)
            throw new MessageException("MTI=" + mtiNum + " is not a request.");
        int lastDigit = mtiNum % 10;
        int respLastDigit = 0;
        switch (lastDigit) {
            case 0:
            case 1: respLastDigit = 0; break;
            case 2:
            case 3: respLastDigit = 2; break; // got issuer or issuer repeat
            case 4:
            case 5: respLastDigit = 4; break; // notification, 2003 version.
            case 6:
            case 7: respLastDigit = 6; break;
            // 8, 9 are reserved.
        }
        String respMti = mti.substring(0, 2);
        respMti += Character.getNumericValue(mti.charAt (2)) + 1;
        respMti += respLastDigit;
        Message copy = copy();
        copy.set(0, respMti);
        return copy;
    }

    // this is a deep copy
    public Message copy() {
        try {
            Message copy = new Message();
            if (attributes != null) {
                copy.attributes = new HashMap<>(attributes);
            }
            if (header != null)
                copy.header = Arrays.copyOf(header, header.length);
            for (Map.Entry<Integer, Object> entry : contents.entrySet()) {
                if (entry.getValue() instanceof Message)
                    copy.contents.put(entry.getKey(), ((Message) entry.getValue()).copy());
                else
                    copy.contents.put(entry.getKey(), entry.getValue());
            }
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
                Object obj = get(field);
                if (obj instanceof Message)
                    obj = ((Message) obj).copy();
                copy.set(field, obj);
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
            return jsonWriter.write(this, true);
        } catch (JsonProcessingException e) {
            return super.toString();
        }
    }
}
