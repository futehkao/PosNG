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
@SuppressWarnings({"unchecked", "java:S3740"})
public class Composite extends Component<Message, Composite> {
    private static final String NOT_CONFIGURED = "Component at %s is not configured.";
    private SortedMap<Integer, Component> components;
    private int extendedBitmap = 65;  // for Banknet, it's third bitmap is located at DE65
    private Map<String, Object> attributes = new HashMap<>(); // this is for class to associate additional information.
    private HeaderField header;
    private BitMapField bitMapField;
    private boolean bitMapInitialized = false;

    public Composite() {
        dataLength(null);
    }

    protected Composite(Composite from) {
        Composite msg = new Composite();
        msg.components = Collections.unmodifiableSortedMap(from.components);
        msg.extendedBitmap = from.extendedBitmap;
        msg.attributes.putAll(from.attributes);
        msg.index(from.index());
        msg.dataLength(dataLength());
    }

    public Map<Integer, Component> getComponents() {
        return components;
    }

    public void setComponents(Map<Integer, Component> components) {
        this.components = new TreeMap<>(components);
    }

    public Component firstComponent() {
        if (components == null || components.isEmpty())
            return null;
        return components.get(components.firstKey());
    }

    public Component lastComponent() {
        if (components == null || components.isEmpty())
            return null;
        return components.get(components.lastKey());
    }

    public <T extends Component> T get(int index) {
        if (components == null)
            return null;
        return (T) components.get(index);
    }

    public <T extends Component> Composite set(int index, T... parts) {
        if (parts == null || parts.length == 0)
            return this;
        int curr = index;
        for (T c : parts) {
            set(curr, c);
            curr++;
        }
        return this;
    }

    public <T extends Component> Composite set(int index, T component) {
        if (component == null) {
            unset(index);
            return this;
        }
        if (components == null)
            components = new TreeMap<>();
        component.index(index);
        try {
            component.validate();
        } catch (MessageException ex) {
            throw new MessageException("Component " + index + " validation error: " + ex.getMessage());
        }
        components.put(index, component);
        if (component instanceof BitMapField) {
            bitMapField = null;
            bitMapInitialized = false;
        }
        return this;
    }

    public Composite unset(int index) {
        if (components == null)
            return this;
        Object component = components.remove(index);
        if (component instanceof BitMapField) {
            bitMapField = null;
            bitMapInitialized = false;
        }
        return this;
    }

    public Collection<Component> components() {
        if (components == null)
            return Collections.emptyList();
        return components.values();
    }

    public int getExtendedBitmap() {
        return extendedBitmap;
    }

    public void setExtendedBitmap(int extendedBitmap) {
        this.extendedBitmap = extendedBitmap;
    }

    public int extendedBitMap() {
        return extendedBitmap;
    }

    public Composite extendedBitMap(int location) {
        extendedBitmap = location;
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

    public HeaderField getHeader() {
        return header;
    }

    public void setHeader(HeaderField header) {
        this.header = header;
        header.validate();
    }

    public HeaderField header() {
        return header;
    }

    public Composite header(HeaderField header) {
        this.header = header;
        header.validate();
        return this;
    }

    protected BitMapField getBitMapField() {
        if (!bitMapInitialized) {
            if (get(0) instanceof BitMapField) {
                bitMapField = get(0);
            } else if (get(1) instanceof BitMapField) {
                bitMapField = get(1);
            }
            bitMapInitialized = true;
        }
        return bitMapField;
    }

    protected int getBitMapFieldIndex() {
        BitMapField b = getBitMapField();
        if (b == null) {
            return Integer.MIN_VALUE;
        } else {
            return b.index();
        }
    }

    public Message defaultValue() {
        return new Message(this);
    }

    public BitMap createBitMap(Message msg) {
        Integer max = msg.lastKey();
        int m = max == null ? 0 : max;

        BitMap primary = new BitMap(m > 64 ? 129 : 65);
        BitMap tertiary = null;
        if (extendedBitmap > 0) {
            if (get(extendedBitmap) instanceof BitMapField && msg.lastKey() > 128) {
                tertiary = new BitMap(65);
                msg.set(extendedBitmap, tertiary);
            } else {
                primary.clear(65);
                primary.clear(extendedBitmap);
            }
        }

        if (m > 64)
            primary.set(1);
        if (m > 128)
            primary.set(65);

        setBits(msg, primary, tertiary);
        return primary;
    }

    protected void setBits(Message msg, BitMap primary, BitMap tertiary) {
        int primaryBitmapIndex = getBitMapFieldIndex();
        msg.unset(primaryBitmapIndex);
        for (Map.Entry<Integer, Object> e : msg.getContents().entrySet()) {
            int pos = e.getKey();
            if (pos <= primaryBitmapIndex || (pos == extendedBitmap && get(pos) instanceof BitMapField))
                continue;

            if (pos > 128 && tertiary != null) {
                tertiary.set(pos - 128);
            } else {
                primary.set(pos);
            }
        }
        msg.set(primaryBitmapIndex, primary);
    }

    protected void writeDEs(OutputStream out, BitMap primary, Message msg) throws IOException {
        if (primary != null) {
            int primaryBitmapIndex = getBitMapFieldIndex();
            for (Map.Entry<Integer, Object> entry : msg.getContents().entrySet()) {
                int i = entry.getKey();
                if (i < primaryBitmapIndex)
                    continue;
                Component component = get(i);
                if (component == null)
                    throw new MessageException(String.format(NOT_CONFIGURED, i));
                component.write(out, entry.getValue());
            }
        } else {
            int firstKey = firstComponent() != null ? firstComponent().index() : -1;
            int lastKey = msg.lastKey();
            for (int i = firstKey; i <= lastKey; i++) {
                Object value = msg.get(i);
                Component component = get(i);
                if (component != null) {
                    if (value == null) {
                        value = component.defaultValue();
                    }
                    component.write(out, value);
                }
            }
        }
    }


    /**
     * Handling of header such as in Visa base 1 should be handled at the caller at the socket level.
     *
     * @param out output stream
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

        // write component 0, could b
        // e an MTI or BitMap
        int bitMapIndex = getBitMapFieldIndex();
        for (int i = 0; i < bitMapIndex; i++) {
            Component comp = get(i);
            if (comp == null)
                continue;
            if (msg.get(i) == null)
                throw new MessageException("Value not present at field=0");
            comp.write(tmpOut, msg.get(i));
        }

        // create bit map
        BitMap primary = null;
        if (getBitMapField() != null) {
            primary = createBitMap(msg);
        }

        // write each component
        writeDEs(tmpOut, primary, msg);

        if (dataLength() != null) {
            ByteArrayOutputStream bout = (ByteArrayOutputStream) tmpOut;
            dataLength().write(out, bout.size());
            out.write(bout.toByteArray());
        }
    }

    protected BitMap readBitMap(InputStream in, Message msg) throws IOException {
        BitMap bitMap = null;
        BitMapField bmf = getBitMapField();
        if (bmf != null) {
            bitMap = bmf.read(in);
            msg.set(bmf.getIndex(), bitMap);
        }
        return bitMap;
    }

    protected void readDEs(InputStream in, BitMap primary, Message msg) throws IOException {
        if (primary != null) {
            readFromBitMap(in, primary, msg);
        } else {
            int maxComponent = lastComponent() != null ? lastComponent().index() : -1;
            for (int i = 1; i <= maxComponent; i++) {
                if (in.available() == 0 && in instanceof ByteArrayInputStream)
                    break;
                Component component = get(i);
                if (component != null)
                    msg.set(i, component.read(in));
            }
        }
    }

    protected void readFromBitMap(InputStream in, BitMap primary, Message msg) throws IOException {
        int start = getBitMapFieldIndex() + 1;
        BitMap bitMap = new BitMap(primary);
        for (int i = bitMap.nextSetBit(start); i >= 0; i = bitMap.nextSetBit(i + 1)) {
            Component component = get(i);
            if (bitMap.get(i)) {
                if (component == null)
                    throw new MessageException(String.format(NOT_CONFIGURED, i));
                Object value = component.read(in);
                msg.set(i, value);

                if (extendedBitmap > 0 && i == extendedBitmap && value instanceof BitMap) {
                    BitMap tertiary = (BitMap) value;
                    bitMap.copy(128, tertiary, 1);
                }
            }
        }
    }

    /**
     * Handling of header such as in Visa base 1 should be handled at the caller at the socket level.
     *
     * @param in input stream
     */
    @Override
    public Message read(InputStream in) throws IOException {
        InputStream tmpIn = in;
        if (dataLength() != null) {
            int len = dataLength().read(in, Short.MAX_VALUE);
            byte[] bytes = Encoder.read(in, len);
            tmpIn = new ByteArrayInputStream(bytes);
        }

        Message msg = new Message(this);
        if (!getAttributes().isEmpty()) {
            msg.setAttributes(getAttributes());
        }
        try {
            // Header for example Visa header with dst and src
            if (header != null) {
                msg.setHeader(header.read(tmpIn));
            }

            // MTI or any field before the bitmap
            int bitMapIndex = getBitMapFieldIndex();
            for (int i = 0; i < bitMapIndex; i++) {
                Component comp = get(i);
                if (comp != null)
                    msg.set(i, comp.read(tmpIn));
            }

            BitMap primary = readBitMap(tmpIn, msg);
            readDEs(tmpIn, primary, msg);

        } catch (Exception e) {
            throw new MessageException(e);
        }
        return msg;
    }
}
