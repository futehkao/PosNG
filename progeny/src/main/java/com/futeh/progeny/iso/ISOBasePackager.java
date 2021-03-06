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

import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import com.futeh.progeny.iso.packager.ISO87APackager;
import com.futeh.progeny.iso.packager.ISO87BPackager;
import com.futeh.progeny.util.LogEvent;
import com.futeh.progeny.util.LogSource;
import com.futeh.progeny.util.Logger;

/**
 * provides base functionality for the actual packagers
 *
 * @author apr@cs.com.uy
 * @version $Id$
 * @see ISO87APackager
 * @see ISO87BPackager
 */
@SuppressWarnings("unchecked")
public abstract class ISOBasePackager implements ISOPackager, LogSource {
    protected ISOFieldPackager[] fld;

    protected Logger logger = null;
    protected String realm = null;
    protected int extendedBitmap = Integer.MIN_VALUE;

    public int getExtendedBitmap() {
        return extendedBitmap;
    }

    public void setExtendedBitmap(int extendedBitmap) {
        this.extendedBitmap = extendedBitmap;
    }

    public void setFieldPackager (ISOFieldPackager[] fld) {
        this.fld = fld;
    }
    /**
     * @return true if BitMap have to be emited
     */
    protected boolean emitBitMap () {
        return (fld[1] instanceof ISOBitMapPackager);
    }
    /**
     * usually 2 for normal fields, 1 for bitmap-less
     * or ANSI X9.2 
     * @return first valid field
     */
    protected int getFirstField() {
        if (!(fld[0] instanceof ISOMsgFieldPackager))
            return (fld[1] instanceof ISOBitMapPackager) ? 2 : 1;
        return 0;
    }

    public ISOBitMap createBitMap(ISOComponent msg, Map<Integer, Object> fields) throws ISOException {
        int m = msg.getMaxField();

        BitSet primary = new BitSet(m > 64 ? 129 : 65);
        BitSet tertiary = null;
        if (extendedBitmap > 0) {
            boolean separateBitMap = fld[extendedBitmap] instanceof ISOBitMapPackager;
            if (separateBitMap) {
                tertiary = new BitSet(65); // 0 to 64
                ISOBitMap isoBitMap = new ISOBitMap(extendedBitmap);
                isoBitMap.setValue(tertiary);
                msg.set(isoBitMap);
                fields.put(extendedBitmap, isoBitMap);
            } else {
                primary.clear(65);
                primary.clear(extendedBitmap);
                fields.remove(extendedBitmap);
            }
        }

        if (m > 64)
            primary.set(1);
        if (m > 128)
            primary.set(65);

        int primaryBitmapIndex = getFirstField() - 1;
        msg.unset(primaryBitmapIndex);
        for (Map.Entry<Integer, Object> e : msg.getChildren().entrySet()) {
            int pos = e.getKey();
            if (pos <= primaryBitmapIndex || (pos == extendedBitmap && fld[pos] instanceof ISOBitMapPackager))
                continue;

            if (pos > 128 && tertiary != null) {
                tertiary.set(pos - 128);
            } else {
                primary.set(pos);
            }
        }
        ISOBitMap primaryBitmap = new ISOBitMap(primaryBitmapIndex);
        primaryBitmap.setValue(primary);
        msg.set(primaryBitmap);
        return primaryBitmap;
    }

    /**
     * @param   m   the Component to pack
     * @return      Message image
     * @exception ISOException
     */
    public byte[] pack (ISOComponent m) throws ISOException {
        LogEvent evt = new LogEvent (this, "pack");
        try {
            if (m.getComposite() != m) 
                throw new ISOException ("Can't call packager on non Composite");

            ISOComponent c;
            ArrayList<byte[]> v = new ArrayList<>(128);
            SortedMap<Integer, Object> fields = m.getChildren();
            int len = 0;
            int first = getFirstField();

            c = (ISOComponent) fields.get (0);
            byte[] b;

            if (first > 0 && c != null) {
                b = fld[0].pack(c);
                len += b.length;
                v.add (b);
            }

            if (emitBitMap()) {
                // BITMAP (-1 in HashTable)
                c = createBitMap(m, fields); // create primary bitmap and maybe extended stored in m and fields.
                b = getBitMapfieldPackager().pack(c);
                len += b.length;
                v.add (b);
            }

            // if Field 1 is a BitMap then we are packing an
            // ISO-8583 message so next field is fld#2.
            // else we are packing an ANSI X9.2 message, first field is 1
            for (Map.Entry<Integer, Object> entry : fields.entrySet()) {
                int i = entry.getKey();
                if (i < first || (c=(ISOComponent) entry.getValue()) == null)
                    continue;
                try {
                    ISOFieldPackager fp = fld[i];
                    if (fp == null)
                        throw new ISOException ("null field packager");
                    b = fp.pack(c);
                    len += b.length;
                    v.add (b);
                } catch (ISOException e) {
                    evt.addMessage ("error packing field "+i);
                    evt.addMessage (c);
                    evt.addMessage (e);
                    throw e;
                }
            }

            int k = 0;
            byte[] d = new byte[len];
            for (int i=0; i<v.size(); i++) {
                b = v.get(i);
                for (int j=0; j<b.length; j++)
                    d[k++] = b[j];
            }
            if (logger != null)  // save a few CPU cycle if no logger available
                evt.addMessage (ISOUtil.hexString (d));
            return d;
        } catch (ISOException e) {
            evt.addMessage (e);
            throw e;
        } finally {
            Logger.log(evt);
        }
    }

    /**
     * @param   m   the Container of this message
     * @param   b   ISO message image
     * @return      consumed bytes
     * @exception ISOException
     */
    public int unpack (ISOComponent m, byte[] b) throws ISOException {
        LogEvent evt = new LogEvent (this, "unpack");
        try {
            if (m.getComposite() != m) 
                throw new ISOException ("Can't call packager on non Composite");
            if (logger != null)  // save a few CPU cycle if no logger available
                evt.addMessage (ISOUtil.hexString (b));

            int consumed = 0;
            if (!(fld[0] instanceof ISOMsgFieldPackager) &&
                !(fld[0] instanceof ISOBitMapPackager))
            {
                ISOComponent mti = fld[0].createComponent(0);
                consumed  = fld[0].unpack(mti, b, 0);
                m.set (mti);
            }
            BitSet bmap = null;
            int maxField = fld.length;
            if (emitBitMap()) {
                ISOBitMap bitmap = new ISOBitMap (-1);
                consumed += getBitMapfieldPackager().unpack(bitmap,b,consumed);
                bmap = (BitSet) bitmap.getValue();
                if (logger != null)
                    evt.addMessage ("<bitmap>"+bmap.toString()+"</bitmap>");
                m.set (bitmap);
                maxField = Math.min(maxField, bmap.size());
            }
                
            for (int i=getFirstField(); i<maxField; i++) {
                try {
                    if (bmap == null && fld[i] == null)
                        continue;

                    if (bmap == null || bmap.get(i)) {
                        if (fld[i] == null)
                            throw new ISOException ("field package '" + i + "' is null");

                        ISOComponent c = fld[i].createComponent(i);
                        consumed += fld[i].unpack (c, b, consumed);
                        if (logger != null) {
                            evt.addMessage ("<unpack fld=\"" + i 
                                +"\" packager=\""
                                +fld[i].getClass().getName()+ "\">");
                            if (c.getValue() instanceof ISOMsg)
                                evt.addMessage (c.getValue());
                            else
                                evt.addMessage ("  <value>" 
                                    +c.getValue().toString()
                                    + "</value>");
                            evt.addMessage ("</unpack>");
                        }
                        m.set(c);

                        // need to check for extended bitmap at extended
                        if (i == extendedBitmap && fld.length > 129 && fld[extendedBitmap] instanceof ISOBitMapPackager) {
                            BitSet extended = (BitSet)((ISOComponent) m.getChildren().get(extendedBitmap)).getValue();
                            for (int j = extended.nextSetBit(1); j >= 0; j = extended.nextSetBit(j + 1)) {
                                bmap.set(128 + j);
                            }
                        }
                    }
                } catch (ISOException e) {
                    System.out.println("error unpacking field "+i);
                    e.printStackTrace(System.out);
                    evt.addMessage (e);
                    throw e;
                }
            }

            if (bmap != null && bmap.get(65) && bmap.get(extendedBitmap) && fld.length > 128 &&
                fld[extendedBitmap] instanceof ISOBitMapPackager)
            {
                bmap= (BitSet) 
                    ((ISOComponent) m.getChildren().get(extendedBitmap)).getValue();
                for (int i=1; i<64; i++) {
                    try {
                        if (bmap == null || bmap.get(i)) {
                            ISOComponent c = fld[i+128].createComponent(i+128);
                            consumed += fld[i+128].unpack (c, b, consumed);
                            if (logger != null) {
                                evt.addMessage ("<unpack fld=\"" + i+128
                                    +"\" packager=\""
                                    +fld[i+128].getClass().getName()+ "\">");
                                evt.addMessage ("  <value>" 
                                    +c.getValue().toString()
                                    + "</value>");
                                evt.addMessage ("</unpack>");
                            }
                            m.set(c);
                        }
                    } catch (ISOException e) {
                        System.out.println("error unpacking field "+i);
                        e.printStackTrace(System.out);
                        throw e;
                    }
                }
            }

            if (b.length != consumed) {
                evt.addMessage (
                    "WARNING: unpack len=" +b.length +" consumed=" +consumed
                );
            }
            return consumed;
        } catch (ISOException e) {
            evt.addMessage (e);
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
            evt.addMessage (e);
            throw new ISOException (e);
        } finally {
            Logger.log (evt);
        }
    }

    public void unpack (ISOComponent m, InputStream in) 
        throws IOException, ISOException 
    {
        LogEvent evt = new LogEvent (this, "unpack");
        try {
            if (m.getComposite() != m) 
                throw new ISOException ("Can't call packager on non Composite");

            if (!(fld[0] instanceof ISOMsgFieldPackager) &&
                !(fld[0] instanceof ISOBitMapPackager))
            {
                ISOComponent mti = fld[0].createComponent(0);
                fld[0].unpack(mti, in);
                m.set (mti);
            }
            BitSet bmap = null;
            int maxField = fld.length;
            if (emitBitMap()) {
                ISOBitMap bitmap = new ISOBitMap (-1);
                getBitMapfieldPackager().unpack(bitmap, in);
                bmap = (BitSet) bitmap.getValue();
                if (logger != null)
                    evt.addMessage ("<bitmap>"+bmap.toString()+"</bitmap>");
                m.set (bitmap);
                maxField = Math.min(maxField, bmap.size());
            }
                
            for (int i=getFirstField(); i<maxField; i++) {
                if (bmap == null && fld[i] == null)
                    continue;

                if (bmap == null || bmap.get(i)) {
                    if (fld[i] == null)
                        throw new ISOException ("field package '" + i + "' is null");

                    ISOComponent c = fld[i].createComponent(i);
                    fld[i].unpack (c, in);
                    if (logger != null) {
                        evt.addMessage ("<unpack fld=\"" + i 
                            +"\" packager=\""
                            +fld[i].getClass().getName()+ "\">");
                        if (c.getValue() instanceof ISOMsg)
                            evt.addMessage (c.getValue());
                        else
                            evt.addMessage ("  <value>" 
                                +c.getValue().toString()
                                + "</value>");
                        evt.addMessage ("</unpack>");
                    }
                    m.set(c);
                    // need to check for extended bitmap at extended
                    if (i == extendedBitmap && fld.length > 129 && fld[extendedBitmap] instanceof ISOBitMapPackager) {
                        BitSet extended = (BitSet)((ISOComponent) m.getChildren().get(extendedBitmap)).getValue();
                        for (int j = extended.nextSetBit(1); j >= 0; j = extended.nextSetBit(j + 1)) {
                            bmap.set(128 + j);
                        }
                    }
                }
            }
            if (bmap != null && bmap.get(65) && bmap.get(extendedBitmap) && fld.length > 128 &&
                    fld[extendedBitmap] instanceof ISOBitMapPackager)
            {
                bmap= (BitSet)
                        ((ISOComponent) m.getChildren().get(extendedBitmap)).getValue();
                for (int i=1; i<64; i++) {
                    if (bmap == null || bmap.get(i)) {
                        ISOComponent c = fld[i+128].createComponent(i);
                        fld[i+128].unpack (c, in);
                        if (logger != null) {
                            evt.addMessage ("<unpack fld=\"" + i+128
                                +"\" packager=\""
                                +fld[i+128].getClass().getName()+ "\">");
                            evt.addMessage ("  <value>" 
                                +c.getValue().toString()
                                + "</value>");
                            evt.addMessage ("</unpack>");
                        }
                        m.set(c);
                    }
                }
            }
        } catch (ISOException e) {
            evt.addMessage (e);
            throw e;
        } catch (EOFException e) {
            throw e;
        } catch (Exception e) {
            evt.addMessage (e);
            throw new ISOException (e);
        } finally {
            Logger.log (evt);
        }
    }
    /**
     * @param   m   the Container (i.e. an ISOMsg)
     * @param   fldNumber the Field Number
     * @return  Field Description
     */
    public String getFieldDescription(ISOComponent m, int fldNumber) {
        return fld[fldNumber].getDescription();
    }
    /**
     * @param   fldNumber the Field Number
     * @return  Field Packager for this field
     */
    public ISOFieldPackager getFieldPackager (int fldNumber) {
        return fld[fldNumber];
    }
    /**
     * @param   fldNumber the Field Number
     * @param   fieldPackager the Field Packager
     */
    public void setFieldPackager 
        (int fldNumber, ISOFieldPackager fieldPackager) 
    {
        fld[fldNumber] = fieldPackager;
    }
    /**
     * @return 128 for ISO-8583, should return 64 for ANSI X9.2
     */
    protected int getMaxValidField() {
        return 128;
    }
    /**
     * @return suitable ISOFieldPackager for Bitmap
     */
    protected ISOFieldPackager getBitMapfieldPackager() {
        return fld[1];
    }
    public void setLogger (Logger logger, String realm) {
        this.logger = logger;
        this.realm  = realm;
    }
    public String getRealm () {
        return realm;
    }
    public Logger getLogger() {
        return logger;
    }
}
