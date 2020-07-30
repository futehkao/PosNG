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
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.futeh.posng.DataElements;
import com.futeh.posng.encoder.Encoder;
import com.futeh.posng.length.DataLength;
import com.futeh.posng.message.*;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unchecked")
public class CompositeBuilder {
    private static final Object SKIP = new Object();
    private static final String CLASS = "class";
    private static final String HEADER = "header";
    private static final String PAD_CHAR = "padChar";
    private static final String PADDING = "padding";
    private static final String ENCODER = "encoder";
    private static ObjectMapper objectMapper = new ObjectMapper()
            .enable(DeserializationFeature.USE_BIG_DECIMAL_FOR_FLOATS)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .configure(JsonParser.Feature.ALLOW_COMMENTS, true)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
            .setSerializationInclusion(JsonInclude.Include.NON_NULL);

    private ClassLoader loader = getClass().getClassLoader();
    private Map<String, Object> definitions = new HashMap<>();
    private Composite composite = new Composite();

    public CompositeBuilder() {
        Class cls = DataElements.class;
        while (cls != null && !cls.equals(Object.class)) {
            Field[] fields = cls.getDeclaredFields();
            for (Field f : fields) {
                if (!Modifier.isPublic(f.getModifiers())
                        || !Modifier.isStatic(f.getModifiers()))
                    continue;
                if (!definitions.containsKey(f.getName())) {
                    try {
                        definitions.put(f.getName(), f.get(null));
                    } catch (IllegalAccessException e) {
                        throw new MessageException(e);
                    }
                }
            }
            cls = cls.getSuperclass();
        }
        Map<String, Object> map = new HashMap<>();
        map.put(CLASS, BitMapField.class.getName());
        definitions.put("BitMap", map);
        definitions.put("CHAR", configureString(StringField.Padding.RIGHT, ' ', Encoder.ASCII));
        definitions.put("ECHAR", configureString(StringField.Padding.RIGHT, ' ', Encoder.EBCDIC));
        definitions.put("NUM", configureString(StringField.Padding.NONE, ' ', Encoder.BCD));
        map.put(CLASS, BinaryField.class.getName());
        definitions.put("BIN", map);
    }

    private Map<String, Object> configureString(StringField.Padding padding, char padChar, Encoder encoder) {
        Map<String, Object> map = new HashMap<>();
        map.put(CLASS, StringField.class.getName());
        map.put(PADDING, padding);
        map.put(PAD_CHAR, padChar);
        map.put(ENCODER, encoder);
        return map;
    }

    private class Tokens {
        int maxLength = -1;
        DataLength dataLength = null;
        StringField.Padding padding = null;
        String padChar = null;
        Encoder encoder;

        Tokens(String[] tokens) {
            int index = 1;
            for (int i = 1; i < tokens.length; i++) {
                if (tokens[i].contains("=")) {
                    parse(tokens[i].trim());
                } else if (!tokens[i].contains("=")) {
                    if (tokens[i].trim().isEmpty())
                        continue;
                    switch (index) {
                        case 1:
                            maxLength = Integer.parseInt(tokens[i]);
                            break;
                        case 2:
                            dataLength = (DataLength) definitions.get(tokens[i]);
                            break;
                        case 3:
                            padding = StringField.Padding.valueOf(tokens[i].toUpperCase());
                            break;
                        case 4:
                            padChar(tokens[i]);
                            break;
                    }
                    index ++;
                }
            }
        }

        void parse(String str) {
            String[] tokens = str.split("=");
            if (tokens.length < 2)
                return;
            tokens[0] = tokens[0].trim();
            tokens[1] = tokens[1].trim();

            if ("maxLength".equalsIgnoreCase(tokens[0])) {
                maxLength = Integer.parseInt(tokens[1]);
            } else if ("dataLength".equalsIgnoreCase(tokens[0])) {
                dataLength = (DataLength) definitions.get(tokens[2]);
            } else if ("encoder".equalsIgnoreCase(tokens[0])) {
                try {
                    encoder = (Encoder) newInstance(tokens[0].trim(), tokens[1].trim());
                } catch (Exception e) {
                    throw new MessageException(e);
                }
            } else if ("padding".equalsIgnoreCase(tokens[0])) {
                padding = StringField.Padding.valueOf(tokens[1].toUpperCase());
            } else if ("padChar".equalsIgnoreCase(tokens[0])) {
                padChar(tokens[1]);
            }
        }

        void padChar(String token) {
            if (token.length() == 3) {
                if (token.charAt(0) == '\'' && token.charAt(2) == '\'') {
                    padChar = "" + token.charAt(1);
                } else {
                    throw new MessageException("Invalid padChar expression: " + token);
                }
            } else if (token.length() == 1) {
                padChar = "" + token.charAt(0);
            } else {
                throw new MessageException("Invalid padChar expression: " + token);
            }
        }

        void configure(com.futeh.posng.message.Field f) {
            if (maxLength >= 0)
                f.setMaxLength(maxLength);
            if (dataLength != null)
                f.setDataLength(dataLength);
            if (encoder != null) {
                f.setEncoder(encoder);
            }
            if (f instanceof StringField) {
                StringField s = (StringField) f;
                if (padding != null)
                    s.setPadding(padding);
                if (padChar != null)
                    s.setPadChar(padChar.charAt(0));
            }
        }
    }

    protected Object fromTokens(String key, String str) throws Exception {
        String tokStr = str.replace("','", "\uFF0C");
        String[] tokens = tokStr.split(",");
        for (int i = 0; i < tokens.length; i++)
            tokens[i] = tokens[i].trim().replace("\uFF0C", "','");
        if (tokens.length == 0)
            throw new MessageException("Empty component for component=" + key);
        Object val = newInstance(key, definitions.get(tokens[0]));
        Tokens t = new Tokens(tokens);

        if (val instanceof com.futeh.posng.message.Field) {
            com.futeh.posng.message.Field f = (com.futeh.posng.message.Field) val;
            t.configure(f);
        } else {
            throw new MessageException(key + " is not a Field.");
        }
        return val;
    }

    public CompositeBuilder config(String str) throws Exception {
        Map<String, Object> map = objectMapper.readValue(str, Map.class);
        Map<String, Map<String, Object>> def = (Map) map.get("definitions");
        Object header = map.get(HEADER);
        Map<String, Map<String, Object>> attr = (Map) map.get("attributes");
        Map<String, Map<String, Object>> components = (Map) map.get("components");

        if (def != null) {
            for (Map.Entry<String, Map<String, Object>> entry : def.entrySet()) {
                definitions.put(entry.getKey(), entry.getValue());
            }
        }

        header(header);

        if (attr != null)
            composite.getAttributes().putAll(attr);

        if (components != null) {
            for (Map.Entry<String, Map<String, Object>> entry : components.entrySet()) {
                component(Integer.parseInt(entry.getKey()), entry.getValue());
            }
        }
        return this;
    }

    public CompositeBuilder header(Object header) throws Exception {
        if (header != null) {
            if (header instanceof Map) {
                Map hdr = (Map) header;
                if (!hdr.containsKey(CLASS))
                    hdr.put(CLASS, BinaryField.class.getName());
            }
            Object h = newInstance(HEADER, header);
            if (!(h instanceof BinaryField))
                throw new MessageException("Header must be an instance of BinaryField");
            BinaryField c = (BinaryField) h;
            composite.header(c);
        }
        return this;
    }

    public CompositeBuilder component(Integer index, Object val) throws Exception {
        composite.component(index, (Component) newInstance(index.toString(), val));
        return this;
    }

    public CompositeBuilder attribute(String key, Object val) throws Exception {
        composite.getAttributes().put(key, val);
        return this;
    }

    public Composite getComposite() {
        return composite;
    }

    public void setComposite(Composite composite) {
        this.composite = composite;
    }

    private Object newInstance(String instanceName, Object obj) throws Exception {
        Object val = obj;
        if (val instanceof Map) {
            val = fromMap(instanceName, (Map) val);
        } else if (val instanceof String) {
            try {
                val = loader.loadClass(val.toString()).getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                val = fromTokens(instanceName, val.toString());
            }
        } else {
            throw new MessageException(instanceName + " configuration: " + val);
        }
        return val;
    }

    private Object fromMap(String instanceName, Map<String, Object> map) throws Exception {
        Object instance;
        Object clsStr = map.get(CLASS);
        if (clsStr == null)
            throw new MessageException("Definition " + instanceName + " missing 'class'");
        if (clsStr.equals(instanceName))
            throw new MessageException("Recursive definition " + instanceName);
        if (definitions.containsKey(clsStr)) {
            Object obj = definitions.get(clsStr);
            if (obj instanceof Map) {
                instance = newInstance(instanceName, (Map) obj);
            } else {
                instance = obj;
                return instance;
            }
        } else {
            Class cls = loader.loadClass(clsStr.toString());
            instance = cls.getDeclaredConstructor().newInstance();
        }

        setProperties(instance, map);
        return instance;
    }

    private void setProperties(Object instance, Map<String, Object> map) throws Exception {
        BeanInfo beanInfo = Introspector.getBeanInfo(instance.getClass());
        PropertyDescriptor[] props = beanInfo.getPropertyDescriptors();
        for (PropertyDescriptor prop : props) {
            Object value = map.get(prop.getName());
            Method setter = prop.getWriteMethod();
            if (prop.getName().equals(CLASS) || value == null || setter == null)
                continue;
            Object field = getField(prop, value);
            if (field != SKIP)
                setter.invoke(instance, field);
        }
    }

    private Object getField(PropertyDescriptor prop, Object value) throws Exception {
        Object field;

        if (StringField.Padding.class.isAssignableFrom(prop.getPropertyType())) {
            StringField.Padding padding = StringField.Padding.valueOf(value.toString().toUpperCase());
            field = padding;
        } else if (value instanceof String // setter takes non-String arg but value is String
                && !String.class.isAssignableFrom(prop.getWriteMethod().getParameterTypes()[0])) {
            if (definitions.containsKey(value.toString())) {
                Object obj = definitions.get(value.toString());
                field = (obj instanceof Map) ? fromMap(prop.getName(), (Map) obj) : obj;
            } else if (Class.class.isAssignableFrom(prop.getWriteMethod().getParameterTypes()[0])) {
                field = loader.loadClass(value.toString());
            } else if (prop.getWriteMethod().getParameterTypes()[0].equals(Character.TYPE)
                    || prop.getWriteMethod().getParameterTypes()[0].equals(Character.class)) {
                String str = value.toString();
                field = str.length() > 0 ? str.charAt(0) : SKIP;
            } else {
                field = loader.loadClass(value.toString()).getDeclaredConstructor().newInstance();
            }
        } else {
            field = value;
        }

        return field;
    }
}
