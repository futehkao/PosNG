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

import com.futeh.posng.encoder.Encoder;

import java.io.*;
import java.util.*;

/**
 * Headers such those in Visa need to be handled at the socket level
 */
@SuppressWarnings("unchecked")
public class Composite extends Component<Message, Composite> {
    private static final String NOT_CONFIGURED = "Component at %s is not configured.";
    private SortedMap<Integer, Component> components;
    private int extendedBitMap = 65;  // for Banknet, it's third bitmap is located at DE65
    private Map<String, Object> attributes = new HashMap<>(); // this is for class to associate additional information.
    private BinaryField header;

    public Composite() {
        dataLength(null);
    }

    protected Composite(Composite from) {
        Composite msg = new Composite();
        msg.components = Collections.unmodifiableSortedMap(from.components);
        msg.extendedBitMap = from.extendedBitMap;
        msg.attributes.putAll(from.attributes);
        msg.index(from.index());
        msg.dataLength(dataLength());
    }

    public SortedMap<Integer, Component> getComponents() {
        return components;
    }

    public void setComponents(SortedMap<Integer, Component> components) {
        this.components = components;
    }

    public int maxComponent() {
        if (components == null || components.isEmpty())
            return 0;
        return components.lastKey();
    }

    public <T extends Component> T component(int index) {
        if (components == null)
            return null;
        return (T) components.get(index);
    }

    public <T extends Component> Composite component(int index, T component) {
        if (components == null)
            components = new TreeMap<>();
        try {
            component.validate();
        } catch (MessageException ex) {
            throw new MessageException("Component " + index + " validation error: " + ex.getMessage());
        }
        component.index(index);
        components.put(index, component);
        return this;
    }

    public Collection<Component> components() {
        if (components == null)
            return Collections.emptyList();
        return components.values();
    }

    public int getExtendedBitMap() {
        return extendedBitMap;
    }

    public void setExtendedBitMap(int extendedBitMap) {
        this.extendedBitMap = extendedBitMap;
    }

    public int extendedBitMap() {
        return extendedBitMap;
    }

    public Composite extendedBitMap(int location) {
        extendedBitMap = location;
        return this;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public Composite setAttribute(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    public BinaryField getHeader() {
        return header;
    }

    public void setHeader(BinaryField header) {
        this.header = header;
        header.validate();
    }

    public BinaryField header() {
        return header;
    }

    public Composite header(BinaryField header) {
        this.header = header;
        header.validate();
        return this;
    }

    public BitMap createBitMap(Message msg) {
        Integer max = msg.lastKey();
        int m = max == null ? 0 : max;

        BitMap primary = new BitMap(128);
        BitMap tertiary = null;
        if (extendedBitMap > 0) {
            boolean separateBitMap = component(extendedBitMap) instanceof BitMapField;
            if (separateBitMap) {
                tertiary = new BitMap(65);
                msg.set(extendedBitMap, tertiary);
            }
        }

        if (m > 64)
            primary.set(1);
        if (m > 128)
            primary.set(65);

        for (Map.Entry<Integer, Object> e : msg.getContents().entrySet()) {
            int pos = e.getKey();
            Component c = component(pos);
            if ((pos == 1 || pos == extendedBitMap) && c instanceof BitMapField)
                continue;

            if (pos > 128 && tertiary != null) {
                tertiary.set(pos - 128);
            } else if (pos != 0) {
                primary.set(pos);
            }
        }
        msg.set(1, primary);
        return primary;
    }

    protected void writeDEs(OutputStream out, BitMap primary, Message msg) throws IOException {
        for (Map.Entry<Integer, Object> entry : msg.getContents().entrySet()) {
            int i = entry.getKey();
            if (primary != null && i > 128)
                break;
            if (i != 0)
                writeDE(out, primary, i, entry.getValue());
        }
    }

    protected void writeDE(OutputStream out, BitMap primary, int i, Object value) throws IOException {
        Component component = component(i);
        if (primary == null && value == null)
            throw new MessageException("Bitmap not present at field=" + i);

        // i = 1 is for the primary bit map so primary.get(1) means whether the next bit map exist.
        if ((primary == null || primary.get(i) || i == 1) && value != null) {
            if (component == null)
                throw new MessageException(String.format(NOT_CONFIGURED, i));
            component.write(out, value);
        }
    }

    protected void writeExtendedDEs(OutputStream out, BitMap primary, Message msg) throws IOException {
        if (primary == null) {
            writeAllExtendedDEs(out, msg);
        } else if (primary.get(65)) {
            BitMap tertiary = primary.length() > 129 ? primary.get(129, 193) : (BitMap) msg.get(extendedBitMap);
            for (int i = tertiary.nextSetBit(1); i >= 0; i = tertiary.nextSetBit(i + 1)) {
                Object value = msg.get(i + 128);
                Component component = component(i + 128);
                if (tertiary.get(i)) {
                    if (component == null)
                        throw new MessageException(String.format(NOT_CONFIGURED, i + 128));
                    component.write(out, value);
                }
            }
        }
    }

    private void writeAllExtendedDEs(OutputStream out, Message msg) throws IOException {
        for (int i = 129; i <= msg.lastKey(); i++) {
            Object value = msg.get(i);
            Component component = component(i);
            if (component != null)
                component.write(out, value);
        }
    }

    /**
     * Handling of header such as in Visa base 1 should be handled at the caller at the socket level.
     * @param out output stream
     * @throws IOException
     */
    @Override
    public void write(OutputStream out, Message msg) throws IOException {
        OutputStream tmpOut = out;
        if (dataLength() != null) {
            tmpOut = new ByteArrayOutputStream();
        }

        if (header != null) {
            header.write(tmpOut, msg.getHeader());
        }

        // write component 0, usually an MTI
        if (component(0) != null) {
            if (msg.get(0) == null)
                throw new MessageException("Value not present  at field=0");
            component(0).write(tmpOut, msg.get(0));
        }

        // create bit map
        BitMap primary = null;
        if (component(1) instanceof BitMapField) {
            primary = createBitMap(msg);
        }

        // write each component
        writeDEs(tmpOut, primary, msg);

        if (primary != null && primary.get(65) && msg.lastKey() > 128) {
            writeExtendedDEs(tmpOut, primary, msg);
        }

        if (dataLength() != null) {
            ByteArrayOutputStream bout = (ByteArrayOutputStream)tmpOut;
            dataLength().write(out, bout.size());
            out.write(bout.toByteArray());
        }
    }

    protected BitMap readBitMap(InputStream in, Message msg) throws IOException {
        BitMap bitMap = null;
        if (component(1) instanceof BitMapField) {
            bitMap = ((BitMapField) component(1)).read(in);
            msg.set(1, bitMap);
        }
        return bitMap;
    }

    protected void readDEs(InputStream in, BitMap bitMap, Message msg) throws IOException {
        if (bitMap != null) {
            readFromBitSet(in, bitMap, msg);
        } else {
            for (int i = 1; i <= 128; i++) {
                Component component = component(i);
                if (component != null)
                    msg.set(i, component.read(in));
            }
        }
    }

    protected void readFromBitSet(InputStream in, BitMap bitMap, Message msg) throws IOException {
        for (int i = bitMap.nextSetBit(2); i >= 0; i = bitMap.nextSetBit(i + 1)) {
            if (i > 128) {
                break;
            }
            Component component = component(i);
            if (bitMap.get(i)) {
                if (component == null)
                    throw new MessageException(String.format(NOT_CONFIGURED, i ));
                Object value = component.read(in);
                msg.set(i, value);
            }
        }
    }

    protected void readExtendedDEs(InputStream in, BitMap primary, Message msg) throws IOException {
        // Some implementation has the third bitmap at a data element
        if (primary == null) {
            readAllExtendedDEs(in, msg);
        } else if (primary.get(65)) {
            BitMap tertiary = primary.length() > 129 ? primary.get(129, 193) : (BitMap) msg.get(extendedBitMap);
            for (int i = tertiary.nextSetBit(1); i >= 0; i = tertiary.nextSetBit(i + 1)) {
                Component component = component(i + 128);
                if (tertiary.get(i)) {
                    if (component == null)
                        throw new MessageException(String.format(NOT_CONFIGURED, i + 128));
                    msg.set(i + 128, component.read(in));
                }
            }
        }
    }

    private void readAllExtendedDEs(InputStream in, Message msg) throws IOException {
        for (int i = 129; i <= maxComponent(); i++) {
            Component component = component(i + 128);
            if (component != null)
                msg.set(i, component.read(in));
        }
    }


    /**
     * Handling of header such as in Visa base 1 should be handled at the caller at the socket level.
     * @param in input stream
     * @throws IOException
     */
    @Override
    public Message read(InputStream in) throws IOException {
        InputStream tmpIn = in;
        if (dataLength() != null) {
            int len = dataLength().read(in, Short.MAX_VALUE);
            byte[] bytes = Encoder.read(in, len);
            tmpIn = new ByteArrayInputStream(bytes);
        }

        Message msg = new Message();
        if (!getAttributes().isEmpty()) {
            msg.setAttributes(new HashMap<>(getAttributes()));
        }
        try {
            // Header for example Visa header with dst and src
            if (header != null) {
                msg.setHeader(header.read(tmpIn));
            }

            // MTI
            Component comp0 = component(0);
            if (comp0 != null) {
                msg.set(0, ((StringField) comp0).read(tmpIn));
            }

            BitMap primary = readBitMap(tmpIn, msg);
            readDEs(tmpIn, primary, msg);
            readExtendedDEs(tmpIn, primary, msg);

        } catch (Exception e) {
            throw new MessageException(e);
        }
        return msg;
    }
}
