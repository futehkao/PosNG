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

import com.futeh.posng.length.DataLength;
import com.futeh.posng.length.FixedLen;

import java.io.*;

/**
 * Models standard Composite design pattern. See https://en.wikipedia.org/wiki/Composite_pattern
 * Instead of Leaf, we use Field to mean 'Leaf' because it better
 * reflects the usage.
 * The Composite class is mapped to 'Composite' in the pattern.
 * @param <T>
 * @param <V>
 */
public abstract class Component<T, V extends Component> {
    private int index = -1;
    private DataLength dataLength = new FixedLen();
    private String desc;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int index() {
        return index;
    }

    public void index(int index) {
        this.index = index;
    }

    public void validate() {
    }

    public abstract T defaultValue();

    public byte[] write(T value) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        write(out, value);
        return out.toByteArray();
    }

    public abstract void write(OutputStream out, T value) throws IOException;

    public T read(byte[] bytes) throws IOException {
        ByteArrayInputStream in = new ByteArrayInputStream(bytes);
        return read(in);
    }

    public abstract T read(InputStream in) throws IOException;

    public DataLength getDataLength() {
        return dataLength;
    }

    public void setDataLength(DataLength dataLength) {
        this.dataLength = dataLength;
    }

    public DataLength dataLength() {
        return dataLength;
    }

    @SuppressWarnings("unchecked")
    public V dataLength(DataLength length) {
        this.dataLength = length;
        return (V) this;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String description) {
        this.desc = description;
    }

    public String desc() {
        return desc;
    }

    public V desc(String description) {
        setDesc(description);
        return (V) this;
    }
}
