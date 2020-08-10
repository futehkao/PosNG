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
import com.futeh.posng.encoder.*;
import com.futeh.posng.length.DataLength;
import com.futeh.posng.message.*;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@SuppressWarnings("unchecked")
public class CompositeBuilder {
    private static final Object SKIP = new Object();
    private static final String CLASS = "class";
    private static final String HEADER = "header";
    private static final String PAD_WITH = "padWith";
    private static final String PADDING = "padding";
    private static final String ENCODER = "encoder";
    private static final String DESC = "desc";

    // definitions
    private static final String BITMAP = "bitmap";
    private static final String BIN = "bin";
    private static final String COMPOSITE = "composite";
    private static final String A_CHAR = "a_char";
    private static final String E_CHAR = "e_char";
    private static final String B_NUM = "b_num";
    private static final String A_NUM = "a_num";
    private static final String E_NUM = "e_num";
    private static final String A_AMT = "a_amt";
    private static final String E_AMT = "e_amt";
    private static final String ASCII = "ascii";
    private static final String EBCDIC = "ebcdic";
    private static final String BCD = "bcd";
    private static final String BCD_PADDED = "bcd_padded";
    private static final String BINARY = "binary";

    private static final String MAX_LENGTH = "maxLength";
    private static final String DATA_LENGTH = "dataLength";

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
        // loading DataElements fields
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
                        definitions.put(f.getName().toLowerCase(), f.get(null));
                        definitions.put(f.getName().toUpperCase(), f.get(null));
                    } catch (IllegalAccessException e) {
                        throw new MessageException(e);
                    }
                }
            }
            cls = cls.getSuperclass();
        }

        // loading DataElements public methods
        for (Method m : DataElements.class.getMethods()) {
            if (!Modifier.isStatic(m.getModifiers()))
                continue;
            if (Component.class.isAssignableFrom(m.getReturnType() )
                && m.getParameterTypes().length == 1
                        && m.getParameterTypes()[0] == Integer.TYPE) {
                Supplier supplier = () -> {
                    try {
                        return m.invoke(null, 0);
                    } catch (Exception e) {
                        throw new MessageException(e);
                    }
                };
                definitions.put(m.getName(), supplier);
                definitions.put(m.getName().toUpperCase(), supplier);
                definitions.put(m.getName().toLowerCase(), supplier);
            } else if (Component.class.isAssignableFrom(m.getReturnType() )
                    && m.getParameterTypes().length == 0) {
                Supplier supplier = () -> {
                    try {
                        return m.invoke(null);
                    } catch (Exception e) {
                        throw new MessageException(e);
                    }
                };
                definitions.put(m.getName(), supplier);
                definitions.put(m.getName().toUpperCase(), supplier);
                definitions.put(m.getName().toLowerCase(), supplier);
            }
        }
    }

    private Map<String, Object> configureAmount(Encoder encoder) {
        Map<String, Object> map = new HashMap<>();
        map.put(CLASS, AmountField.class.getName());
        map.put(ENCODER, encoder);
        return map;
    }

    private Map<String, Object> configureString(Padding padding, String padWith, Encoder encoder) {
        Map<String, Object> map = new HashMap<>();
        map.put(CLASS, StringField.class.getName());
        map.put(PADDING, padding);
        map.put(PAD_WITH, padWith);
        map.put(ENCODER, encoder);
        return map;
    }

    protected Object fromTokens(String key, String str) {
        String tokStr = str.replace("\\,", "\uFF0C");
        String[] tokens = tokStr.split(",");
        for (int i = 0; i < tokens.length; i++)
            tokens[i] = tokens[i].trim().replace("\uFF0C", ",");
        if (tokens.length == 0)
            throw new MessageException("Empty component for component=" + key);

        Object component = definitions.get(tokens[0]);
        if (component == null) {
            component = definitions.get(tokens[0].toLowerCase());
        }
        if (component == null) {
            component = definitions.get(tokens[0].toUpperCase());
        }
        if (component == null) {
            throw new MessageException("No definition found for " + tokens[0] + " in " + str);
        }

        Object val = newInstance(key, component);
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

        if (def != null) {
            for (Map.Entry<String, Map<String, Object>> entry : def.entrySet()) {
                definitions.put(entry.getKey(), entry.getValue());
            }
        }

        configure(composite, map);

        return this;
    }

    protected void configure(Composite comp, Map<String, Object> map) {
        Object header = map.get(HEADER);
        Map<String, Map<String, Object>> attr = (Map) map.get("attributes");
        Map<String, Map<String, Object>> components = (Map) map.get("components");

        header(header);

        if (attr != null)
            comp.getAttributes().putAll(attr);

        if (components != null) {
            for (Map.Entry<String, Map<String, Object>> entry : components.entrySet()) {
                set(comp, Integer.parseInt(entry.getKey()), entry.getValue());
            }
        }
    }

    public CompositeBuilder header(Object header) {
        if (header != null) {
            if (header instanceof Map) {
                Map hdr = (Map) header;
                if (!hdr.containsKey(CLASS))
                    hdr.put(CLASS, BinaryField.class.getName());
            }
            Object h = newInstance(HEADER, header);
            if (!(h instanceof HeaderField))
                throw new MessageException("Header must be an instance of HeaderField");
            HeaderField c = (HeaderField) h;
            composite.header(c);
        }
        return this;
    }

    public CompositeBuilder set(int index, Object val) {
        composite.set(index, (Component) newInstance("" + index, val));
        return this;
    }

    public CompositeBuilder set(Composite comp, int index, Object val) {
        comp.set(index, (Component) newInstance("" + index, val));
        return this;
    }

    public CompositeBuilder attribute(String key, Object val) {
        composite.getAttributes().put(key, val);
        return this;
    }

    public Composite getComposite() {
        return composite;
    }

    public void setComposite(Composite composite) {
        this.composite = composite;
    }

    private Object newInstance(String instanceName, Object obj) {
        Object val = obj;
        if (val instanceof Map) {
            val = fromMap(instanceName, (Map) val);
        } else if (val instanceof String) {
            String str = val.toString();
            try {
                if (str.contains(",") || str.contains("="))
                    val = fromTokens(instanceName, val.toString());
                else
                    val = loader.loadClass(str).getDeclaredConstructor().newInstance();
            } catch (Exception ex) {
                val = loadClassField(instanceName, val.toString());
            }
        } else if (val instanceof Supplier) {
            val = ((Supplier) val).get();
        } else {
            throw new MessageException(instanceName + " configuration: " + val);
        }
        return val;
    }

    private Object loadClassField(String instanceName, String str) {
        Object val;
        int idx = str.lastIndexOf('.');
        if (idx > 0 && idx < str.length() - 2) {
            String clsstr = str.substring(0, idx);
            String field = str.substring(idx + 1);
            try {
                Class cls = loader.loadClass(clsstr);
                val = cls.getDeclaredField(field).get(null);
            } catch (Exception e) {
                val = fromTokens(instanceName, str);
            }
        } else {
            val = fromTokens(instanceName, str);
        }
        return val;
    }

    private Object fromMap(String instanceName, Map<String, Object> map) {
        Object instance;
        Object clsStr = map.get(CLASS);
        if (clsStr == null)
            throw new MessageException("Definition " + instanceName + " missing 'class'");
        if (clsStr.equals(instanceName))
            throw new MessageException("Recursive definition " + instanceName);
        if (definitions.containsKey(clsStr)) {
            instance = fromDefinition(instanceName, clsStr.toString());
            if (instance == definitions.get(clsStr)) // existing instance, don't modifiy, returns immediately
                return instance;
        } else {
            try {
                Class cls = loader.loadClass(clsStr.toString());
                instance = cls.getDeclaredConstructor().newInstance();
            } catch (Exception e) {
                throw new MessageException(e);
            }
        }

        if (instance instanceof Composite) {
            configure((Composite) instance, map);
        } else {
            setProperties(instance, map);
        }
        return instance;
    }

    private void setProperties(Object instance, Map<String, Object> map) {
        BeanInfo beanInfo = null;
        try {
            beanInfo = Introspector.getBeanInfo(instance.getClass());
        } catch (IntrospectionException e) {
            throw new MessageException(e);
        }
        PropertyDescriptor[] props = beanInfo.getPropertyDescriptors();
        for (PropertyDescriptor prop : props) {
            Object value = map.get(prop.getName());
            Method setter = prop.getWriteMethod();
            if (prop.getName().equals(CLASS) || value == null || setter == null)
                continue;
            Object field = null;
            try {
                field = convert(prop, value);
            } catch (Exception exception) {
                throw new MessageException(exception);
            }
            if (field != SKIP) {
                setProperty(instance, setter, field);
            }
        }
    }

    private void setProperty(Object instance, Method setter, Object value) {
        try {
            setter.invoke(instance, value);
        } catch (Exception ex) {
            throw new MessageException(ex);
        }
    }

    private Object convert(PropertyDescriptor prop, Object value) throws Exception {
        Object field;

        if (Padding.class.isAssignableFrom(prop.getPropertyType())) {
            Padding padding = Padding.valueOf(value.toString().toUpperCase());
            field = padding;
        } else if (value instanceof String // setter takes non-String arg but value is String
                && !String.class.isAssignableFrom(prop.getWriteMethod().getParameterTypes()[0])) {
            if (definitions.containsKey(value.toString())) {
                field = fromDefinition(prop.getName(), value.toString());
            } else if (Class.class.isAssignableFrom(prop.getWriteMethod().getParameterTypes()[0])) {
                field = loader.loadClass(value.toString());
            } else if (prop.getWriteMethod().getParameterTypes()[0].equals(Character.TYPE)
                    || prop.getWriteMethod().getParameterTypes()[0].equals(Character.class)) {
                String str = value.toString();
                field = str.length() > 0 ? str.charAt(0) : SKIP;
            } else if (prop.getWriteMethod().getParameterTypes()[0].equals(Byte.TYPE)
                    || prop.getWriteMethod().getParameterTypes()[0].equals(Byte.class)) {
                String str = value.toString();
                byte[] bytes = Hex.encode(str);
                field = bytes.length > 0 ? bytes[0] : SKIP;
            } else {
                try {
                    field = loader.loadClass(value.toString()).getDeclaredConstructor().newInstance();
                } catch (Exception ex) {
                    String str = value.toString();
                    int idx = str.lastIndexOf('.');
                    if (idx > 0 && idx < str.length() - 2) {
                        String clsstr = str.substring(0, idx);
                        String f = str.substring(idx + 1);
                        Class cls = loader.loadClass(clsstr);
                        field = cls.getDeclaredField(f).get(null);
                    } else {
                        throw new MessageException("Cannot convert " + str + " to " + prop.getName()
                                + " of type " + prop.getPropertyType());
                    }
                }
            }
        } else {
            field = value;
        }

        return field;
    }

    private <T> T fromDefinition(String name, String value) {
        Object obj = definitions.get(value);
        return (T) ((obj instanceof Map) ? fromMap(name, (Map) obj)
                : (obj instanceof Supplier) ? ((Supplier) obj).get() : obj);
    }

    private class Tokens {
        int maxLength = -1;
        DataLength dataLength = null;
        Padding padding = null;
        String padWith = null;
        Encoder encoder;
        String comment;

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
                            dataLength = fromDefinition(null, tokens[i]);
                            break;
                        case 3:
                            padding = Padding.valueOf(tokens[i].toUpperCase());
                            break;
                        case 4:
                            padWith(tokens[i]);
                            break;
                    }
                    index++;
                }
            }
        }

        void parse(String str) {
            String[] tokens = str.split("=");
            if (tokens.length < 2)
                return;
            tokens[0] = tokens[0].trim();
            tokens[1] = tokens[1].trim();

            if (MAX_LENGTH.equalsIgnoreCase(tokens[0])) {
                maxLength = Integer.parseInt(tokens[1]);
            } else if (DATA_LENGTH.equalsIgnoreCase(tokens[0])) {
                if (definitions.containsKey(tokens[1])) {
                    dataLength = fromDefinition(tokens[0], tokens[1]);
                } else {
                    dataLength = (DataLength) newInstance(tokens[0], tokens[1]);
                }
            } else if (ENCODER.equalsIgnoreCase(tokens[0])) {
                if (definitions.containsKey(tokens[1])) {
                    encoder = fromDefinition(tokens[0], tokens[1]);
                } else {
                    encoder = (Encoder) newInstance(tokens[0], tokens[1]);
                }
            } else if (PADDING.equalsIgnoreCase(tokens[0])) {
                padding = Padding.valueOf(tokens[1].toUpperCase());
            } else if (PAD_WITH.equalsIgnoreCase(tokens[0])) {
                padWith(tokens[1]);
            } else if (DESC.equalsIgnoreCase(tokens[0])) {
                comment = tokens[1];
            }
        }

        void padWith(String token) {
            padWith = token;
        }

        void configure(com.futeh.posng.message.Field f) {
            if (maxLength >= 0)
                f.setMaxLength(maxLength);
            if (dataLength != null)
                f.setDataLength(dataLength);
            if (encoder != null) {
                f.setEncoder(encoder);
            }
            if (padding != null)
                f.setPadding(padding);
            if (comment != null)
                f.setDesc(comment);

            if (f instanceof StringField) {
                StringField s = (StringField) f;
                if (padWith != null)
                    s.setPadWith(padWith.charAt(0));
            } else if (f instanceof BinaryField) {
                BinaryField s = (BinaryField) f;
                if (padWith != null && padWith.length() > 0) {
                    byte[] bytes = Hex.encode(padWith);
                    s.setPadWith(bytes[0]);
                }
            }
        }
    }
}
