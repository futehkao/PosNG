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

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.futeh.posng.message.serialization.Alias;

import java.util.BitSet;
import java.util.SortedSet;
import java.util.TreeSet;

@JsonPropertyOrder("class")
public class BitMap implements Alias {
    private BitSet bitSet;

    public BitMap() {
        bitSet = new BitSet();
    }

    public BitMap(int size) {
        bitSet = new BitSet(size);
    }

    public int length() {
        return bitSet.length();
    }

    public void clear() {
        bitSet.clear();
    }

    public void clear(int bitIndex) {
        bitSet.clear(bitIndex);
    }

    public void clear(int fromIndex, int toIndex) {
        bitSet.clear(fromIndex, toIndex);
    }

    public void set(int bitIndex) {
        bitSet.set(bitIndex);
    }

    public boolean get(int bitIndex) {
        return bitSet.get(bitIndex);
    }

    public BitMap get(int from, int to) {
        BitMap bitMap = new BitMap();
        bitMap.bitSet = bitSet.get(from, to);
        return bitMap;
    }

    public int nextSetBit(int bitIndex) {
        return bitSet.nextSetBit(bitIndex);
    }

    public SortedSet<Integer> getBitMap() {
        SortedSet<Integer> bits = new TreeSet<>();
        for (int i = bitSet.nextSetBit(0); i >= 0; i = bitSet.nextSetBit(i + 1)) {
            bits.add(i);
        }
        return bits;
    }

    public void setBitMap(SortedSet<Integer> bits) {
        bitSet = new BitSet(bits.last() + 1);
        for (int b : bits) {
            bitSet.set(b);
        }
    }

    public String toString() {
        return bitSet.toString();
    }
}
