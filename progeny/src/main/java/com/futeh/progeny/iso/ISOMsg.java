/*
 * Copyright (c) 2000 jPOS.org.  All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution,
 *    if any, must include the following acknowledgment:
 *    "This product includes software developed by the jPOS project
 *    (http://www.jpos.org/)". Alternately, this acknowledgment may
 *    appear in the software itself, if and wherever such third-party
 *    acknowledgments normally appear.
 *
 * 4. The names "jPOS" and "jPOS.org" must not be used to endorse
 *    or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    license@jpos.org.
 *
 * 5. Products derived from this software may not be called "jPOS",
 *    nor may "jPOS" appear in their name, without prior written
 *    permission of the jPOS project.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE JPOS PROJECT OR ITS CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
 * OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT,
 * STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING
 * IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the jPOS Project.  For more
 * information please see <http://www.jpos.org/>.
 */

package com.futeh.progeny.iso;

import com.futeh.progeny.iso.header.BaseHeader;
import com.futeh.progeny.iso.packager.XMLPackager;
import com.futeh.progeny.util.Loggeable;

import java.io.*;
import java.lang.ref.WeakReference;
import java.util.BitSet;
import java.util.SortedMap;
import java.util.StringTokenizer;
import java.util.TreeMap;

/**
 * implements <b>Composite</b>
 * whithin a <b>Composite pattern</b>
 *
 * @author apr@cs.com.uy
 * @version $Id$
 * @see ISOComponent
 * @see ISOField
 */
public class ISOMsg extends ISOComponent
        implements Cloneable, Loggeable, Externalizable {
    protected SortedMap<Integer, Object> fields;
    protected int maxField;
    protected ISOPackager packager;
    protected boolean dirty, maxFieldDirty;
    protected int direction;
    protected ISOHeader header;
    protected int fieldNumber = -1;
    public static final int INCOMING = 1;
    public static final int OUTGOING = 2;
    private static final long serialVersionUID = 4306251831901413975L;
    private WeakReference sourceRef;

    /**
     * Creates an ISOMsg
     */
    public ISOMsg() {
        fields = new TreeMap<>();
        maxField = -1;
        dirty = true;
        maxFieldDirty = true;
        direction = 0;
        header = null;
    }

    /**
     * Creates a nested ISOMsg
     */
    public ISOMsg(int fieldNumber) {
        this();
        setFieldNumber(fieldNumber);
    }

    /**
     * changes this Component field number<br>
     * Use with care, this method does not change
     * any reference held by a Composite.
     *
     * @param fieldNumber new field number
     */
    public void setFieldNumber(int fieldNumber) {
        this.fieldNumber = fieldNumber;
    }

    /**
     * Creates an ISOMsg with given mti
     *
     * @param mti Msg's MTI
     */
    public ISOMsg(String mti) {
        this();
        try {
            setMTI(mti);
        } catch (Exception e) {
            // should never happen
        }
    }

    /**
     * Sets the direction information related to this message
     *
     * @param direction can be either ISOMsg.INCOMING or ISOMsg.OUTGOING
     */
    public void setDirection(int direction) {
        this.direction = direction;
    }

    /**
     * Sets an optional message header image
     *
     * @param b header image
     */
    public void setHeader(byte[] b) {
        header = new BaseHeader(b);
    }

    public void setHeader(ISOHeader header) {
        this.header = header;
    }

    /**
     * get optional message header image
     *
     * @return message header image (may be null)
     */
    public byte[] getHeader() {
        return (header != null) ? header.pack() : null;
    }

    /**
     * Return this messages ISOHeader
     */
    public ISOHeader getISOHeader() {
        return header;
    }

    /**
     * @return the direction (ISOMsg.INCOMING or ISOMsg.OUTGOING)
     * @see ISOChannel
     */
    public int getDirection() {
        return direction;
    }

    /**
     * @return true if this message is an incoming message
     * @see ISOChannel
     */
    public boolean isIncoming() {
        return direction == INCOMING;
    }

    /**
     * @return true if this message is an outgoing message
     * @see ISOChannel
     */
    public boolean isOutgoing() {
        return direction == OUTGOING;
    }

    /**
     * @return the max field number associated with this message
     */
    public int getMaxField() {
        if (maxFieldDirty)
            recalcMaxField();
        return maxField;
    }

    private void recalcMaxField() {
        maxField = 0;
        for (Object key : fields.keySet()) {
            if (key instanceof Integer)
                maxField = Math.max(maxField, ((Integer) key).intValue());
        }
        maxFieldDirty = false;
    }

    /**
     * @param p - a peer packager
     */
    public void setPackager(ISOPackager p) {
        packager = p;
    }

    /**
     * @return the peer packager
     */
    public ISOPackager getPackager() {
        return packager;
    }

    /**
     * Set a field within this message
     *
     * @param c - a component
     */
    public ISOMsg set(ISOComponent c) {
        Integer i = (Integer) c.getKey();
        fields.put(i, c);
        if (i.intValue() > maxField)
            maxField = i.intValue();
        dirty = true;
        return this;
    }

    /**
     * Creates an ISOField associated with fldno within this ISOMsg
     *
     * @param fldno field number
     * @param value field value
     */
    public ISOMsg set(int fldno, String value) {
        if (value != null)
            set(new ISOField(fldno, value));
        else
            unset(fldno);
        return this;
    }

    /**
     * Creates an ISOBinaryField associated with fldno within this ISOMsg
     *
     * @param fldno field number
     * @param value field value
     */
    public ISOMsg set(int fldno, byte[] value) {
        if (value != null)
            set(new ISOBinaryField(fldno, value));
        else
            unset(fldno);
        return this;
    }

    public ISOMsg set(String path, String value) {
        set(path, (Object) value);
        return this;
    }

    public ISOMsg set(String path, byte[] value) {
        set(path, (Object) value);
        return this;
    }

    private void set(String path, Object value) {
        String[] tokens = path.split("\\.");
        if (tokens.length == 0)
            return;

        ISOMsg current = this;
        for (int i = 0; i < tokens.length - 1; i++) {
            int index = Integer.parseInt(tokens[i].trim());
            Object obj = current.getValue(index);
            if (obj == null) {
                ISOMsg msg = new ISOMsg(index);
                current.set(msg);
                current = msg;
            } else if (obj instanceof ISOMsg) {
                current = (ISOMsg) obj;
            } else {
                throw new IllegalArgumentException("Invalid path " + index);
            }
        }
        if (value instanceof String)
            current.set(Integer.parseInt(tokens[tokens.length - 1].trim()), (String) value);
        else if (value instanceof byte[])
            current.set(Integer.parseInt(tokens[tokens.length - 1].trim()), (byte[]) value);
        else if (value == null)
            current.unset(Integer.parseInt(tokens[tokens.length - 1].trim()));
        else
            throw new IllegalArgumentException("Invalid argument type " + value);
    }

    /**
     * Unset a field if it exists, otherwise ignore.
     *
     * @param fldno - the field number
     */
    public void unset(int fldno) {
        if (fields.remove(new Integer(fldno)) != null)
            dirty = maxFieldDirty = true;
    }

    /**
     * Unsets several fields at once
     *
     * @param flds - array of fields to be unset from this ISOMsg
     */
    public void unset(int[] flds) {
        for (int i = 0; i < flds.length; i++)
            unset(flds[i]);
    }

    /**
     * In order to interchange <b>Composites</b> and <b>Leafs</b> we use
     * getComposite(). A <b>Composite component</b> returns itself and
     * a Leaf returns null.
     *
     * @return ISOComponent
     */
    public ISOComponent getComposite() {
        return this;
    }

    /**
     * We dont' really care for toplevel ISOMsg because ISOBasePackager recalculates it.
     * For other types of packager, just just use -1 field
     * setup BitMap
     *
     */
    public void recalcBitMap() {
        if (!dirty)
            return;
        int maxFieldRecalculated = getMaxField();
        BitSet bmap = new BitSet(maxFieldRecalculated);

        for (int i = 1; i <= maxFieldRecalculated; i++)
            if (fields.get(i) != null)
                bmap.set(i);
        set(new ISOBitMap(-1, bmap));

        dirty = false;
    }

    /**
     * clone fields
     */
    public SortedMap<Integer, Object> getChildren() {
        return (SortedMap<Integer, Object>) ((TreeMap) fields).clone();
    }

    /**
     * pack the message with the current packager
     *
     * @return the packed message
     * @throws ISOException
     */
    public byte[] pack() throws ISOException {
        synchronized (this) {
            recalcBitMap();
            return packager.pack(this);
        }
    }

    /**
     * unpack a message
     *
     * @param b - raw message
     * @return consumed bytes
     * @throws ISOException
     */
    public int unpack(byte[] b) throws ISOException {
        synchronized (this) {
            return packager.unpack(this, b);
        }
    }

    public void unpack(InputStream in) throws IOException, ISOException {
        synchronized (this) {
            packager.unpack(this, in);
        }
    }

    /**
     * dump the message to a PrintStream. The output is sorta
     * XML, intended to be easily parsed.
     * <br>
     * Each component is responsible for its own dump function,
     * ISOMsg just calls dump on every valid field.
     *
     * @param p      - print stream
     * @param indent - optional indent string
     */
    public void dump(PrintStream p, String indent) {
        ISOComponent c;
        p.print(indent + "<" + XMLPackager.ISOMSG_TAG);
        switch (direction) {
            case INCOMING:
                p.print(" direction=\"incoming\"");
                break;
            case OUTGOING:
                p.print(" direction=\"outgoing\"");
                break;
        }
        if (fieldNumber != -1)
            p.print(" " + XMLPackager.ID_ATTR + "=\"" + fieldNumber + "\"");
        p.println(">");
        String newIndent = indent + "  ";

        if (header instanceof Loggeable)
            ((Loggeable) header).dump(p, newIndent);

        for (int i = 0; i <= maxField; i++) {
            if ((c = (ISOComponent) fields.get(new Integer(i))) != null)
                c.dump(p, newIndent);
            //
            // Uncomment to include bitmaps within logs
            // 
            // if (i == 0) {
            //  if ((c = (ISOComponent) fields.get (new Integer (-1))) != null)
            //    c.dump (p, newIndent);
            // }
            //
        }

        p.println(indent + "</" + XMLPackager.ISOMSG_TAG + ">");
    }

    /**
     * get the component associated with the given field number
     *
     * @param fldno the Field Number
     * @return the Component
     */
    public ISOComponent getComponent(int fldno) {
        return (ISOComponent) fields.get(new Integer(fldno));
    }

    /**
     * Return the object value associated with the given field number
     *
     * @param fldno the Field Number
     * @return the field Object
     */
    public Object getValue(int fldno) {
        ISOComponent c = getComponent(fldno);
        return c != null ? c.getValue() : null;
    }

    public Object getValue(String path) {
        String[] tokens = path.split("\\.");
        if (tokens.length == 0)
            return null;
        ISOMsg current = this;
        for (int i = 0; i < tokens.length - 1; i++) {
            int index = Integer.parseInt(tokens[i].trim());
            Object obj = current.getValue(index);
            if (obj == null)
                return null;
            else if (obj instanceof ISOMsg) {
                current = (ISOMsg) obj;
            } else {
                throw new IllegalArgumentException("Invalid path " + path);
            }
        }
        return current.getValue(Integer.parseInt(tokens[tokens.length - 1].trim()));
    }

    public byte[] getBytes (int fldno) {
        return (byte[]) getValue(fldno);
    }

    public byte[] getBytes (String path) {
        return (byte[]) getValue(path);
    }

    /**
     * Return the String value associated with the given ISOField number
     *
     * @param fldno the Field Number
     * @return field's String value
     */
    public String getString(int fldno) {
        String s = null;
        if (hasField(fldno)) {
            try {
                Object obj = getValue(fldno);
                if (obj instanceof String)
                    s = (String) obj;
                else if (obj instanceof byte[])
                    s = ISOUtil.hexString((byte[]) obj);
            } catch (Exception e) {
                // ignore Exception - return null
            }
        }
        return s;
    }

    public String getString(String path) {
        String str = null;
        try {
            Object obj = getValue(path);
            if (obj instanceof String)
                str = (String) obj;
            else if (obj instanceof byte[])
                str = ISOUtil.hexString((byte[]) obj);
        } catch (Exception ex) {

        }
        return str;
    }

    /**
     * Check if a given field is present
     *
     * @param fldno the Field Number
     * @return boolean indicating the existence of the field
     */
    public boolean hasField(int fldno) {
        return fields.get(fldno) != null;
    }

    /**
     * Check if all fields are present
     *
     * @param fields an array of fields to check for presence
     * @return true if all fields are present
     */
    public boolean hasFields(int[] fields) {
        for (int i = 0; i < fields.length; i++)
            if (!hasField(fields[i]))
                return false;
        return true;
    }

    public boolean hasField (String path) {
        return getValue(path) != null;
    }

    /**
     * Don't call setValue on an ISOMsg. You'll sure get
     * an ISOException. It's intended to be used on Leafs
     *
     * @see ISOField
     */
    public void setValue(Object obj) {
        throw new UnsupportedOperationException("setValue N/A in ISOMsg");
    }

    public Object clone() {
        try {
            ISOMsg m = (ISOMsg) super.clone();
            m.fields = (SortedMap<Integer, Object>) ((TreeMap) fields).clone();
            if (header != null)
                m.header = (ISOHeader) header.clone();
            return m;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    /**
     * Partially clone an ISOMsg
     *
     * @param fields int array of fields to go
     * @return new ISOMsg instance
     */
    public Object clone(int[] fields) {
        try {
            ISOMsg m = (ISOMsg) super.clone();
            m.fields = new TreeMap<>();
            for (int i = 0; i < fields.length; i++) {
                if (hasField(fields[i])) {
                    try {
                        m.set(getComponent(fields[i]));
                    } catch (Exception e) {
                        // it should never happen
                    }
                }
            }
            return m;
        } catch (CloneNotSupportedException e) {
            throw new InternalError();
        }
    }

    /**
     * add all fields present on received parameter to this ISOMsg<br>
     * please note that received fields take precedence over
     * existing ones (simplifying card agent message creation
     * and template handling)
     *
     * @param m ISOMsg to merge
     */
    public void merge(ISOMsg m) {
        for (int i = 0; i <= m.getMaxField(); i++)
            try {
                if (m.hasField(i))
                    set(m.getComponent(i));
            } catch (Exception e) {
                // should never happen
            }
    }

    /**
     * @return a string suitable for a log
     */
    public String toString() {
        StringBuffer s = new StringBuffer();
        if (isIncoming())
            s.append("<-- ");
        else if (isOutgoing())
            s.append("--> ");
        else
            s.append("    ");

        try {
            s.append((String) getValue(0));
            if (hasField(11)) {
                s.append(' ');
                s.append((String) getValue(11));
            }
            if (hasField(41)) {
                s.append(' ');
                s.append((String) getValue(41));
            }
        } catch (Exception e) {
        }
        return s.toString();
    }

    public Object getKey() {
        if (fieldNumber != -1)
            return new Integer(fieldNumber);
        throw new UnsupportedOperationException("This is not a subField");
    }

    public Object getValue() {
        return this;
    }

    /**
     * @return true on inner messages
     */
    public boolean isInner() {
        return fieldNumber > -1;
    }

    /**
     * @param mti new MTI
     */
    public ISOMsg setMTI(String mti) {
        if (isInner())
            throw new UnsupportedOperationException("can't setMTI on inner message");
        set(new ISOField(0, mti));
        return this;
    }

    /**
     * moves a field (renumber)
     *
     * @param oldFieldNumber old field number
     * @param newFieldNumber new field number
     */
    public void move(int oldFieldNumber, int newFieldNumber) {
        ISOComponent c = getComponent(oldFieldNumber);
        unset(oldFieldNumber);
        if (c != null) {
            c.setFieldNumber(newFieldNumber);
            set(c);
        } else
            unset(newFieldNumber);
    }

    /**
     * @return current MTI
     */
    public String getMTI() {
        if (isInner())
            throw new UnsupportedOperationException("can't getMTI on inner message");
        else if (!hasField(0))
            throw new UnsupportedOperationException("MTI not available");
        return (String) getValue(0);
    }

    /**
     * @return true if message "seems to be" a request
     */
    public boolean isRequest() {
        return Character.getNumericValue(getMTI().charAt(2)) % 2 == 0;
    }

    /**
     * @return true if message "seems not to be" a request
     */
    public boolean isResponse() {
        return !isRequest();
    }

    /**
     * @return true if message is Retransmission
     */
    public boolean isRetransmission() {
        return getMTI().charAt(3) == '1';
    }

    /**
     * sets an appropiate response MTI.
     * <p>
     * i.e. 0100 becomes 0110<br>
     * i.e. 0201 becomes 0210<br>
     * i.e. 1201 becomes 1210<br>
     *
     */
    public void setResponseMTI() {
        if (!isRequest())
            throw new UnsupportedOperationException("not a request - can't set response MTI");

        String mti = getMTI();
        char c1 = mti.charAt(3);
        char c2 = '0';
        switch (c1) {
            case '0':
            case '1':
                c2 = '0';
                break;
            case '2':
            case '3':
                c2 = '2';
                break;
            case '4':
            case '5':
                c2 = '4';
                break;

        }
        set(new ISOField(0,
                        mti.substring(0, 2)
                                + (Character.getNumericValue(getMTI().charAt(2)) + 1) + c2
                )
        );
    }

    /**
     * sets an appropiate retransmission MTI<br>
     *
     */
    public void setRetransmissionMTI() {
        if (!isRequest())
            throw new UnsupportedOperationException("not a request");

        set(new ISOField(0, getMTI().substring(0, 3) + "1"));
    }

    protected void writeHeader(ObjectOutput out) throws IOException {
        int len = header.getLength();
        if (len > 0) {
            out.writeByte('H');
            out.writeShort(len);
            out.write(header.pack());
        }
    }

    protected void readHeader(ObjectInput in)
            throws IOException, ClassNotFoundException {
        byte[] b = new byte[in.readShort()];
        in.readFully(b);
        setHeader(b);
    }

    protected void writeDirection(ObjectOutput out) throws IOException {
        out.writeByte('D');
        out.writeByte(direction);
    }

    protected void readDirection(ObjectInput in)
            throws IOException, ClassNotFoundException {
        direction = in.readByte();
    }

    public void writeExternal(ObjectOutput out) throws IOException {
        out.writeByte(0);  // reserved for future expansion (version id)
        out.writeShort(fieldNumber);

        if (header != null)
            writeHeader(out);
        if (direction > 0)
            writeDirection(out);

        for (Object e : fields.values()) {
            ISOComponent c = (ISOComponent) e;
            if (c instanceof ISOMsg) {
                out.writeByte('M');
                ((Externalizable) c).writeExternal(out);
            } else if (c instanceof ISOBinaryField) {
                out.writeByte('B');
                ((Externalizable) c).writeExternal(out);
            } else if (c instanceof ISOField) {
                out.writeByte('F');
                ((Externalizable) c).writeExternal(out);
            }
        }
        out.writeByte('E');
    }

    public void readExternal(ObjectInput in)
            throws IOException, ClassNotFoundException {
        in.readByte();  // ignore version for now
        fieldNumber = in.readShort();
        byte fieldType;
        ISOComponent c;
        try {
            while ((fieldType = in.readByte()) != 'E') {
                c = null;
                switch (fieldType) {
                    case 'F':
                        c = new ISOField();
                        break;
                    case 'B':
                        c = new ISOBinaryField();
                        break;
                    case 'M':
                        c = new ISOMsg();
                        break;
                    case 'H':
                        readHeader(in);
                        break;
                    case 'D':
                        readDirection(in);
                        break;
                    default:
                        throw new IOException("malformed ISOMsg");
                }
                if (c != null) {
                    ((Externalizable) c).readExternal(in);
                    set(c);
                }
            }
        } catch (Exception e) {
            throw new IOException(e.getMessage());
        }
    }

    /**
     * Let this ISOMsg object hold a weak reference to an ISOSource
     * (usually used to carry a reference to the incoming ISOChannel)
     *
     * @param source an ISOSource
     */
    public void setSource(ISOSource source) {
        this.sourceRef = new WeakReference(source);
    }

    /**
     * @return an ISOSource or null
     */
    public ISOSource getSource() {
        return (sourceRef != null) ? (ISOSource) sourceRef.get() : null;
    }
}

