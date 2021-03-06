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

package com.futeh.progeny.iso.channel;

import java.io.IOException;
import java.net.ServerSocket;

import com.futeh.progeny.util.LogEvent;
import com.futeh.progeny.util.Logger;
import com.futeh.progeny.iso.BaseChannel;
import com.futeh.progeny.iso.ISOException;
import com.futeh.progeny.iso.ISOMsg;
import com.futeh.progeny.iso.ISOPackager;
import com.futeh.progeny.iso.ISOUtil;

/**
 * Implements an ISOChannel able to exchange messages with
 * ACI's BASE24 over a TCP link, modified from BASE24ISOChannel 
 * by Victor A. Salaman (salaman@teknos.com) .<br>
 * An instance of this class exchanges messages by means of an
 * intermediate 'port server' as described in the
 * <a href="/doc/javadoc/overview-summary.html">Overview</a> page.
 * @author apr@cs.com.uy
 * @author salaman@teknos.com
 *
 * @version $Id$
 *
 * @see ISOMsg
 * @see ISOException
 * @see ISOChannel
 */

public class BASE24TCPChannel extends BaseChannel {
    /**
     * Public constructor (used by Class.forName("...").newInstance())
     */
    public BASE24TCPChannel () {
        super();
    }
    /**
     * Construct client ISOChannel
     * @param host  server TCP Address
     * @param port  server port number
     * @param p     an ISOPackager
     * @see ISOPackager
     */
    public BASE24TCPChannel (String host, int port, ISOPackager p) {
        super(host, port, p);
    }
    /**
     * Construct server ISOChannel
     * @param p     an ISOPackager
     * @see ISOPackager
     * @exception IOException
     */
    public BASE24TCPChannel (ISOPackager p) throws IOException {
        super(p);
    }
    /**
     * constructs a server ISOChannel associated with a Server Socket
     * @param p     an ISOPackager
     * @param serverSocket where to accept a connection
     * @exception IOException
     * @see ISOPackager
     */
    public BASE24TCPChannel (ISOPackager p, ServerSocket serverSocket) 
        throws IOException
    {
        super(p, serverSocket);
    }
    /**
     * @param m the Message to send (in this case it is unused)
     * @param bytes   message data (ignored)
     * @exception IOException
     */
    protected void sendMessageTrailler(ISOMsg m, byte[] bytes) throws IOException {
        serverOut.write (3);
    }
    protected void sendMessageLength(int len) throws IOException {
        len++;  // one byte trailler
        serverOut.write (len >> 8);
        serverOut.write (len);
    }
    protected int getMessageLength() throws IOException, ISOException {
        int l = 0;
        byte[] b = new byte[2];
        Logger.log (new LogEvent(this, "get-message-length"));
        while (l == 0) {
            serverIn.readFully(b,0,2);
            l = ((((int)b[0])&0xFF) << 8) | (((int)b[1])&0xFF);
            if (l == 0) {
                serverOut.write(b);
                serverOut.flush();
            }
        }
        Logger.log (new LogEvent (this, "got-message-length", Integer.toString(l)));
        return l - 1;   // trailler length
    }
    protected void getMessageTrailler() throws IOException {
        Logger.log (new LogEvent (this, "get-message-trailler"));
        byte[] b = new byte[1];
        serverIn.readFully(b,0,1);
        Logger.log (new LogEvent (this, "got-message-trailler", ISOUtil.hexString(b)));
    }
}

