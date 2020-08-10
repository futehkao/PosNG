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
import com.futeh.posng.encoder.Hex;
import com.futeh.posng.message.serialization.Alias;
import com.futeh.posng.message.serialization.JsonWriter;

import java.util.*;

@JsonPropertyOrder("class")
public class Message implements Alias {
    private static JsonWriter jsonWriter = new JsonWriter();
    private byte[] header;
    private Map<String, Object> attributes;
    private SortedMap<Integer, Object> contents = new TreeMap<>();
    private Composite composite;

    public Message() {
    }

    public Message(Composite composite) {
        this.composite = composite;
    }

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
        this.attributes = new WeakHashMap<>(attributes);
    }

    @JsonIgnore
    public Composite getComposite() {
        return composite;
    }

    @JsonIgnore
    public void setComposite(Composite composite) {
        this.composite = composite;
    }

    public Object getAttribute(String key) {
        if (attributes == null)
            return null;
        return attributes.get(key);
    }

    public Message setAttribute(String key, Object value) {
        if (attributes == null)
            attributes = new WeakHashMap<>();
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
        return (T) contents.get(index);
    }

    public <T> T get(String path) {
        String[] tokens = path.split("\\.");
        if (tokens.length == 0)
            return null;
        Message current = this;
        for (int i = 0; i < tokens.length - 1; i++) {
            int index = Integer.parseInt(tokens[i].trim());
            Object obj = current.get(index);
            if (obj == null)
                return null;
            else if (obj instanceof Message) {
                current = (Message) obj;
            } else {
                throw new MessageException("Invalid path " + path);
            }
        }
        return (T) current.get(Integer.parseInt(tokens[tokens.length - 1].trim()));
    }

    public String getString(int index) {
        return toString(get(index));
    }

    public String getString(String path) {
        return toString(get(path));
    }

    private String toString(Object val) {
        if (val != null) {
            if (val instanceof String)
                return val.toString();
            else if (val instanceof byte[])
                return Hex.decode((byte[]) val);
            else
                return val.toString();
        }
        return null;
    }

    public Message set(int index, Object value) {
        if (value == null)
            unset(index);
        else {
            if (getComposite() != null) {
                Component component = getComposite().get(index);
                if (component == null)
                    throw new MessageException("No component definition at " + index);
                if (!component.defaultValue().getClass().isAssignableFrom(value.getClass())) {
                    throw new MessageException("Illegal assignment at " + index
                            + " cannot convert " + value.getClass()
                            + " but " + component.defaultValue().getClass());
                }
            }
            contents.put(index, value);
        }
        return this;
    }

    public Message set(String path, Object value) {
        String[] tokens = path.split("\\.");
        if (tokens.length == 0)
            return null;
        Message current = this;
        for (int i = 0; i < tokens.length - 1; i++) {
            int index = Integer.parseInt(tokens[i].trim());
            Object obj = current.get(index);
            if (obj == null) {
                Composite comp = null;
                if (current.getComposite() != null) {
                    Component component = current.getComposite().get(index);
                    if (!(component instanceof Composite)) {
                        StringBuilder builder = new StringBuilder();
                        for (int j = 0; j <= i; j++) {
                            builder.append(tokens[j]);
                            if (j != i)
                                builder.append(".");
                        }
                        throw new MessageException("Expecting composite at " + builder.toString());
                    }
                    comp = (Composite) component;
                }
                Message msg = new Message(comp);
                current.set(index, msg);
                current = msg;
            } else if (obj instanceof Message) {
                current = (Message) obj;
            } else {
                throw new MessageException("Invalid path " + path);
            }
        }
        current.set(Integer.parseInt(tokens[tokens.length - 1].trim()), value);
        return this;
    }

    public Message unset(int index) {
        contents.remove(index);
        return this;
    }

    public String getMTI() {
        return get(0).toString();
    }

    // https://en.wikipedia.org/wiki/ISO_8583
    // Message Origin
    public Message copyResponse() {
        if (!contents.containsKey(0))
            throw new MessageException("MTI not present");

        String mti = contents.get(0).toString();
        int msgFunc = Character.getNumericValue(mti.charAt(2)); // message function according to the wiki
        if (msgFunc % 2 != 0)
            throw new MessageException("MTI=" + mti + " is not a request.");

        int msgOrigin = 0; // the last digit is called message origin
        switch (msgFunc) {
            case 0:
            case 1:
                msgOrigin = 0;
                break;
            case 2:
            case 3:
                msgOrigin = 2;
                break; // got issuer or issuer repeat
            case 4:
            case 5:
                msgOrigin = 4;
                break; // notification, 2003 version.
            case 6:
            case 7:
                msgOrigin = 6;
                break;
            // 8, 9 are reserved.
        }
        String respMti = mti.substring(0, 2);
        respMti += (msgFunc + 1);
        respMti += msgOrigin;
        Message copy = copy();
        copy.set(0, respMti);

        if (getComposite() != null && getComposite().header() != null) {
            copy.setHeader(getComposite().header().forResponse(copy.header));
        }
        return copy;
    }

    // this is a deep copy
    public Message copy() {
        try {
            Message copy = new Message(composite);
            if (attributes != null) {
                copy.attributes = new HashMap<>(attributes);
            }

            if (header != null) {
                copy.header = Arrays.copyOf(header, header.length);
            }

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
            Message copy = new Message(composite);
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
