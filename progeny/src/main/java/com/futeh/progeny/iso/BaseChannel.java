/*
 * Copyright (c) 2004 jPOS.org 
 *
 * See terms of license at http://jpos.org/license.html
 *
 */

package com.futeh.progeny.iso;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;

import com.futeh.progeny.core.Configurable;
import com.futeh.progeny.core.Configuration;
import com.futeh.progeny.core.ConfigurationException;
import com.futeh.progeny.core.ReConfigurable;
import com.futeh.progeny.iso.channel.CSChannel;
import com.futeh.progeny.iso.header.BaseHeader;
import com.futeh.progeny.util.LogEvent;
import com.futeh.progeny.util.LogSource;
import com.futeh.progeny.util.Logger;
import com.futeh.progeny.util.NameRegistrar;

/*
 * BaseChannel was ISOChannel. Now ISOChannel is an interface
 * Revision: 1.34 Date: 2000/04/08 23:54:55 
 */

/**
 * ISOChannel is an abstract class that provides functionality that
 * allows the transmision and reception of ISO 8583 Messages
 * over a TCP/IP session.
 * <p>
 * It is not thread-safe, ISOMUX takes care of the
 * synchronization details
 * <p>
 * ISOChannel is Observable in order to suport GUI components
 * such as ISOChannelPanel.
 * <br>
 * It now support the new Logger architecture so we will
 * probably setup ISOChannelPanel to be a LogListener insteado
 * of being an Observer in future releases.
 * 
 * @author Alejandro P. Revilla
 * @author Bharavi Gade
 * @version $Revision$ $Date$
 * @see ISOMsg
 * @see ISOMUX
 * @see ISOException
 * @see CSChannel
 * @see Logger
 *
 */
@SuppressWarnings("unchecked")
public abstract class BaseChannel extends Observable 
    implements FilteredChannel, ClientChannel, ServerChannel, FactoryChannel,
        LogSource, ReConfigurable, BaseChannelMBean
{
    private Socket socket;
    private String host, localIface;
    private int port, timeout, localPort;
    protected boolean usable;
    protected boolean overrideHeader;
    private String name;
    // private int serverPort = -1;
    protected DataInputStream serverIn;
    protected DataOutputStream serverOut;
    protected ISOPackager packager;
    protected ServerSocket serverSocket = null;
    protected List incomingFilters, outgoingFilters;
    protected ISOClientSocketFactory socketFactory = null;

    protected int[] cnt;

    protected Logger logger = null;
    protected String realm = null;
    protected String originalRealm = null;
    protected byte[] header = null;

    /**
     * constructor shared by server and client
     * ISOChannels (which have different signatures)
     */
    public BaseChannel () {
        super();
        cnt = new int[SIZEOF_CNT];
        name = "";
        incomingFilters = new ArrayList();
        outgoingFilters = new ArrayList();
        setHost (null, 0);
    }

    /**
     * constructs a client ISOChannel
     * @param host  server TCP Address
     * @param port  server port number
     * @param p     an ISOPackager
     * @see ISOPackager
     */
    public BaseChannel (String host, int port, ISOPackager p) {
        this();
        setHost(host, port);
        setPackager(p);
    }
    /**
     * constructs a server ISOChannel
     * @param p     an ISOPackager
     * @exception IOException
     * @see ISOPackager
     */
    public BaseChannel (ISOPackager p) throws IOException {
        this();
        setPackager (p);
    }

    /**
     * constructs a server ISOChannel associated with a Server Socket
     * @param p     an ISOPackager
     * @param serverSocket where to accept a connection
     * @exception IOException
     * @see ISOPackager
     */
    public BaseChannel (ISOPackager p, ServerSocket serverSocket) 
        throws IOException 
    {
        this();
        setPackager (p);
        setServerSocket (serverSocket);
    }

    /**
     * initialize an ISOChannel
     * @param host  server TCP Address
     * @param port  server port number
     */
    public void setHost(String host, int port) {
        this.host = host;
        this.port = port;
    }
    
    /**
     * initialize an ISOChannel
     * @param iface  server TCP Address
     * @param port  server port number
     */
    public void setLocalAddress (String iface, int port) {
        this.localIface = iface;
        this.localPort = port;
    }
    

    /**
     * @param host to connect (client ISOChannel)
     */
    public void setHost (String host) {
        this.host = host;
    }
    /**
     * @param port to connect (client ISOChannel)
     */
    public void setPort (int port) {
        this.port = port;
    }
    /**
     * @return hostname (may be null)
     */
    public String getHost() {
        return host;
    }
    /**
     * @return port number
     */
    public int getPort() {
        return port;
    }
    /**
     * set Packager for channel
     * @param p     an ISOPackager
     * @see ISOPackager
     */
    public void setPackager(ISOPackager p) {
        this.packager = p;
    }

    /**
     * @return current packager
     */
    public ISOPackager getPackager() {
        return packager;
    }

    /**
     * Associates this ISOChannel with a server socket
     * @param sock where to accept a connection
     */
    public void setServerSocket (ServerSocket sock) {
        setHost (null, 0);
        this.serverSocket = sock;
        name = "";
    }

    /**
     * reset stat info
     */
    public void resetCounters() {
        for (int i=0; i<SIZEOF_CNT; i++)
            cnt[i] = 0;
    }
   /**
    * @return counters
    */
    public int[] getCounters() {
        return cnt;
    }
    /**
     * @return the connection state
     */
    public boolean isConnected() {
        return socket != null && usable;
    }
    /**
     * setup I/O Streams from socket
     * @param socket a Socket (client or server)
     * @exception IOException
     */
    protected void connect (Socket socket) 
        throws IOException, SocketException
    {
        this.socket = socket;
        applyTimeout();
        setLogger(getLogger(), getOriginalRealm() + 
            "/" + socket.getInetAddress().getHostAddress() + ":" 
            + socket.getPort()
        );
        serverIn = new DataInputStream (
            new BufferedInputStream (socket.getInputStream ())
        );
        serverOut = new DataOutputStream(
            new BufferedOutputStream(socket.getOutputStream(), 2048)
        );
        usable = true;
        cnt[CONNECT]++;
        setChanged();
        notifyObservers();
    }
    /**
     * factory method pattern (as suggested by Vincent.Greene@amo.com)
     * @throws IOException
     * Use Socket factory if exists. If it is missing create a normal socket
     * @see ISOClientSocketFactory
     */
    protected Socket newSocket() throws IOException {
        try {
            if (socketFactory != null)
                return socketFactory.createSocket (host, port);
            else {
                if (timeout > 0) {
                    Socket s = new Socket();
                    s.connect (
                        new InetSocketAddress (host, port),
                        timeout
                    );
                    return s;
                } else if (localIface == null && localPort == 0){
                    return new Socket(host,port);
                } else {
                    InetAddress addr = (localIface == null) ?
                        InetAddress.getLocalHost() : 
                        InetAddress.getByName(localIface);
                    return new Socket(host, port, addr, localPort);
                }
            }
        } catch (ISOException e) {
            throw new IOException (e.getMessage());
        }
    }
    /**
     * @return current socket
     */
    public Socket getSocket() {
        return socket;
    }
    /**
     * @return current serverSocket
     */
    public ServerSocket getServerSocket() {
        return serverSocket;
    }

    /** 
     * sets socket timeout (as suggested by 
     * Leonard Thomas, leonard@rhinosystemsinc.com
     * @param timeout in milliseconds
     * @throws SocketException
     */
    public void setTimeout (int timeout) throws SocketException {
        this.timeout = timeout;
        applyTimeout();
    }
    public int getTimeout () {
        return timeout;
    }
    protected void applyTimeout () throws SocketException {
        if (timeout >= 0 && socket != null) 
            socket.setSoTimeout (timeout);
    }
    /**
     * Connects client ISOChannel to server
     * @exception IOException
     */
    public void connect () throws IOException {
        LogEvent evt = new LogEvent (this, "connect");
        try {
            if (serverSocket != null) {
                accept(serverSocket);
                evt.addMessage ("local port "+serverSocket.getLocalPort()
                    +" remote host "+socket.getInetAddress());
            }
            else {
                evt.addMessage (host+":"+port);
                connect(newSocket ());
            }
            applyTimeout();
            Logger.log (evt);
        } catch (ConnectException e) {
            Logger.log (new LogEvent (this, "connection-refused",
                getHost()+":"+getPort())
            );
        } catch (IOException e) {
            evt.addMessage (e.getMessage ());
            Logger.log (evt);
            throw e;
        }
    }

    /**
     * Accepts connection 
     * @exception IOException
     */
    public void accept(ServerSocket s) throws IOException {
        // if (serverPort > 0)
        //    s = new ServerSocket (serverPort);
        // else
        //     serverPort = s.getLocalPort();

        connect(s.accept());

        // Warning - closing here breaks ISOServer, we need an
        // accept that keep ServerSocket open.
        // s.close();
    }

    /**
     * @param b - new Usable state (used by ISOMUX internals to
     * flag as unusable in order to force a reconnection)
     */
    public void setUsable(boolean b) {
        Logger.log (new LogEvent (this, "usable", new Boolean (b)));
        usable = b;
    }
   /**
    * allow subclasses to override default packager
    * on outgoing messages
    * @param m outgoing ISOMsg
    * @return ISOPackager
    */
    protected ISOPackager getDynamicPackager (ISOMsg m) {
        return packager;
    }

   /**
    * allow subclasses to override default packager
    * on outgoing messages
    * @param image incoming message image
    * @return ISOPackager
    */
    protected ISOPackager getDynamicPackager (byte[] image) {
        return packager;
    }

    /** 
     * Allow subclasses to override the Default header on
     * incoming messages.
     */
    protected ISOHeader getDynamicHeader (byte[] image) {
        return image != null ? 
            new BaseHeader(image) : null;
    }
    protected void sendMessageLength(int len) throws IOException { }
    protected void sendMessageHeader(ISOMsg m, int len) throws IOException { 
        if (!overrideHeader && m.getHeader() != null)
            serverOut.write(m.getHeader());
        else if (header != null) 
            serverOut.write(header);
    }

    protected void sendMessageTrailler(ISOMsg m, byte[] b) throws IOException { }
    protected void getMessageTrailler() throws IOException { }
    protected void getMessage (byte[] b, int offset, int len) throws IOException { 
        serverIn.readFully(b, offset, len);
    }
    protected int getMessageLength() throws IOException, ISOException {
        return -1;
    }
    protected int getHeaderLength() { 
        return header != null ? header.length : 0;
    }
    protected int getHeaderLength(byte[] b) { return 0; }
    protected byte[] streamReceive() throws IOException {
        return new byte[0];
    }
    protected void sendMessage (byte[] b, int offset, int len) 
        throws IOException
    {
        serverOut.write(b, offset, len);
    }

    // adapted from the send below.
    public void send (byte[] b) throws IOException, ISOException
    {
        LogEvent evt = new LogEvent (this, "send");
        try {
            if (!isConnected())
                throw new ISOException ("unconnected ISOChannel");
            synchronized (serverOut) {
                serverOut.write(b);
                serverOut.flush();
            }
            cnt[TX]++;
            setChanged();
        } catch (ISOException e) {
            evt.addMessage (e);
            throw e;
        } catch (Exception e) {
            evt.addMessage (e);
            throw new ISOException ("unexpected exception", e);
        } finally {
            Logger.log (evt);
        }
    }

    /**
     * sends an ISOMsg over the TCP/IP session
     * @param m the Message to be sent
     * @exception IOException
     * @exception ISOException
     * @exception ISOFilter.VetoException
     */
    public void send (ISOMsg m) 
        throws IOException, ISOException, ISOFilter.VetoException
    {
        LogEvent evt = new LogEvent (this, "send");
        evt.addMessage (m);
        try {
            if (!isConnected())
                throw new ISOException ("unconnected ISOChannel");
            m.setDirection(ISOMsg.OUTGOING);
            m = applyOutgoingFilters (m, evt);
            m.setDirection(ISOMsg.OUTGOING); // filter may have drop this info
            m.setPackager (getDynamicPackager(m));
            byte[] b = m.pack();
            synchronized (serverOut) {
                sendMessageLength(b.length + getHeaderLength());
                sendMessageHeader(m, b.length);
                sendMessage (b, 0, b.length);
                sendMessageTrailler(m, b);
                serverOut.flush ();
            }
            cnt[TX]++;
            setChanged();
            notifyObservers(m);
        } catch (ISOFilter.VetoException e) {
            evt.addMessage (e);
            throw e;
        } catch (ISOException e) {
            evt.addMessage (e);
            throw e;
        } catch (IOException e) {
            evt.addMessage (e);
            throw e;
        } catch (Exception e) {
            evt.addMessage (e);
            throw new ISOException ("unexpected exception", e);
        } finally {
            Logger.log (evt);
        }
    }
    protected boolean isRejected(byte[] b) {
        // VAP Header support - see VAPChannel
        return false;
    }
    protected boolean shouldIgnore (byte[] b) {
        // VAP Header support - see VAPChannel
        return false;
    }
    protected ISOMsg createMsg () {
        return new ISOMsg();
    }
	
    /**
     * Reads in a message header.
     *
     * @param hLen The Length og the reader to read
     * @return The header bytes that were read in
     */
    protected byte[] readHeader(int hLen) throws IOException {
        byte[] header = new byte[hLen];
        serverIn.readFully(header, 0, hLen);
        return header;
    }
    /**
     * Waits and receive an ISOMsg over the TCP/IP session
     * @return the Message received
     * @exception IOException
     * @exception ISOException
     */
    public ISOMsg receive() throws IOException, ISOException {
        byte[] b=null;
        byte[] header=null;
        LogEvent evt = new LogEvent (this, "receive");
        ISOMsg m = createMsg ();
        m.setSource (this);
        try {
            if (!isConnected())
                throw new ISOException ("unconnected ISOChannel");

            synchronized (serverIn) {
                int len  = getMessageLength();
                int hLen = getHeaderLength();

                if (len == -1) {
                    if (hLen > 0) {
                        header = readHeader(hLen);
                    }
                    b = streamReceive();
                }
                else if (len > 0 && len <= 10000) {
                    if (hLen > 0) {
                        // ignore message header (TPDU)
                        // Note header length is not necessarily equal to hLen (see VAPChannel)
                        header = readHeader(hLen);
                        len -= header.length;
                    }
                    b = new byte[len];
                    getMessage (b, 0, len);
                    getMessageTrailler();
                }
                else
                    throw new ISOException(
                        "receive length " +len + " seems strange");
            }
            m.setPackager (getDynamicPackager(b));
            m.setHeader (getDynamicHeader(header));
            if (b.length > 0 && !shouldIgnore (header))  // Ignore NULL messages
                m.unpack (b);
            m.setDirection(ISOMsg.INCOMING);
            m = applyIncomingFilters (m, header, b, evt);
            m.setDirection(ISOMsg.INCOMING);
            evt.addMessage (m);
            cnt[RX]++;
            setChanged();
            notifyObservers(m);
        } catch (ISOException e) {
            evt.addMessage (e);
            if (header != null) {
                evt.addMessage ("--- header ---");
                evt.addMessage (ISOUtil.hexdump (header));
            }
            if (b != null) {
                evt.addMessage ("--- data ---");
                evt.addMessage (ISOUtil.hexdump (b));
            }
            throw e;
        } catch (EOFException e) {
            if (socket != null)
                socket.close ();
            evt.addMessage ("<peer-disconnect/>");
            throw e;
        } catch (InterruptedIOException e) {
            if (socket != null)
                socket.close ();
            evt.addMessage ("<io-timeout/>");
            throw e;
        } catch (IOException e) { 
            if (socket != null)
                socket.close ();
            if (usable) 
                evt.addMessage (e);
            throw e;
        } catch (Exception e) { 
            evt.addMessage (m);
            evt.addMessage (e);
            throw new ISOException ("unexpected exception", e);
        } finally {
            Logger.log (evt);
        }
        return m;
    }
    /**
     * Low level receive
     * @param b byte array
     * @exception IOException
     */
    public int getBytes (byte[] b) throws IOException {
        return serverIn.read (b);
    }
    /**
     * disconnects the TCP/IP session. The instance is ready for
     * a reconnection. There is no need to create a new ISOChannel<br>
     * @exception IOException
     */
    public void disconnect () throws IOException {
        LogEvent evt = new LogEvent (this, "disconnect");
        if (serverSocket != null) 
            evt.addMessage ("local port "+serverSocket.getLocalPort()
                +" remote host "+serverSocket.getInetAddress());
        else
            evt.addMessage (host+":"+port);
        try {
            usable = false;
            setChanged();
            notifyObservers();
            if (serverIn != null) {
                try {
                    serverIn.close();
                } catch (IOException ex) { evt.addMessage (ex); }
                serverIn  = null;
            }
            if (serverOut != null) {
                try {
                    serverOut.close();
                } catch (IOException ex) { evt.addMessage (ex); }
                serverOut = null;
            }
            if (socket != null) {
                try {
                    socket.setSoLinger (true, 0);
                } catch (SocketException e) {
                    // safe to ignore - can be closed already
                }
                socket.close ();
            }
            Logger.log (evt);
        } catch (IOException e) {
            evt.addMessage (e);
            Logger.log (evt);
            throw e;
        }
        socket = null;
    }   
    /**
     * Issues a disconnect followed by a connect
     * @exception IOException
     */
    public void reconnect() throws IOException {
        disconnect();
        connect();
    }
    public void setLogger (Logger logger, String realm) {
        this.logger = logger;
        this.realm  = realm;
        if (originalRealm == null)
            originalRealm = realm;
    }
    public String getRealm () {
        return realm;
    }
    public Logger getLogger() {
        return logger;
    }
    public String getOriginalRealm() {
        return originalRealm == null ? 
            this.getClass().getName() : originalRealm;
    }
    /**
     * associates this ISOChannel with a name using NameRegistrar
     * @param name name to register
     * @see NameRegistrar
     */
    public void setName (String name) {
        this.name = name;
        NameRegistrar.register ("channel."+name, this);
    }
    /**
     * @return this ISOChannel's name ("" if no name was set)
     */
    public String getName() {
        return this.name;
    }
    /**
     * @param filter filter to add
     * @param direction ISOMsg.INCOMING, ISOMsg.OUTGOING, 0 for both
     */
    public void addFilter (ISOFilter filter, int direction) {
        switch (direction) {
            case ISOMsg.INCOMING :
                incomingFilters.add (filter);
                break;
            case ISOMsg.OUTGOING :
                outgoingFilters.add (filter);
                break;
            case 0 :
                incomingFilters.add (filter);
                outgoingFilters.add (filter);
                break;
        }
    }
    /**
     * @param filter incoming filter to add
     */
    public void addIncomingFilter (ISOFilter filter) {
        addFilter (filter, ISOMsg.INCOMING);
    }
    /**
     * @param filter outgoing filter to add
     */
    public void addOutgoingFilter (ISOFilter filter) {
        addFilter (filter, ISOMsg.OUTGOING);
    }

    /**
     * @param filter filter to add (both directions, incoming/outgoing)
     */
    public void addFilter (ISOFilter filter) {
        addFilter (filter, 0);
    }
    /**
     * @param filter filter to remove
     * @param direction ISOMsg.INCOMING, ISOMsg.OUTGOING, 0 for both
     */
    public void removeFilter (ISOFilter filter, int direction) {
        switch (direction) {
            case ISOMsg.INCOMING :
                incomingFilters.remove (filter);
                break;
            case ISOMsg.OUTGOING :
                outgoingFilters.remove (filter);
                break;
            case 0 :
                incomingFilters.remove (filter);
                outgoingFilters.remove (filter);
                break;
        }
    }
    /**
     * @param filter filter to remove (both directions)
     */
    public void removeFilter (ISOFilter filter) {
        removeFilter (filter, 0);
    }
    /**
     * @param filter incoming filter to remove
     */
    public void removeIncomingFilter (ISOFilter filter) {
        removeFilter (filter, ISOMsg.INCOMING);
    }
    /**
     * @param filter outgoing filter to remove
     */
    public void removeOutgoingFilter (ISOFilter filter) {
        removeFilter (filter, ISOMsg.OUTGOING);
    }
    protected ISOMsg applyOutgoingFilters (ISOMsg m, LogEvent evt) 
        throws ISOFilter.VetoException
    {
        Iterator iter  = outgoingFilters.iterator();
        while (iter.hasNext())
            m = ((ISOFilter) iter.next()).filter (this, m, evt);
        return m;
    }
    protected ISOMsg applyIncomingFilters (ISOMsg m, LogEvent evt) 
        throws ISOFilter.VetoException
    {
        return applyIncomingFilters (m, null, null, evt);
    }
    protected ISOMsg applyIncomingFilters (ISOMsg m, byte[] header, byte[] image, LogEvent evt) 
        throws ISOFilter.VetoException
    {
        Iterator iter  = incomingFilters.iterator();
        while (iter.hasNext()) {
            ISOFilter f = (ISOFilter) iter.next ();
            if (image != null && (f instanceof RawIncomingFilter))
                m = ((RawIncomingFilter)f).filter (this, m, header, image, evt);
            else
                m = f.filter (this, m, evt);
        }
        return m;
    }
   /**
    * Implements Configurable<br>
    * Properties:<br>
    * <ul>
    * <li>host - destination host (if ClientChannel)
    * <li>port - port number      (if ClientChannel)
    * <li>local-iface - local interfase to use (if ClientChannel)
    * <li>local-port - local port to bind (if ClientChannel)
    * </ul>
    * (host not present indicates a ServerChannel)
    *
    * @param cfg Configuration
    * @throws ConfigurationException
    */
    public void setConfiguration (Configuration cfg)
        throws ConfigurationException 
    {
        String h    = cfg.get    ("host");
        int port    = cfg.getInt ("port");
        if (h != null && h.length() > 0) {
            if (port == 0)
                throw new ConfigurationException 
                    ("invalid port for host '"+h+"'");
            setHost (h, port);
            setLocalAddress (cfg.get("local-iface", null),cfg.getInt("local-port"));
        }
        overrideHeader = cfg.getBoolean ("override-header", false);
        if (socketFactory != this && socketFactory instanceof Configurable)
            ((Configurable)socketFactory).setConfiguration (cfg);
        try {
            setTimeout (cfg.getInt ("timeout"));
        } catch (SocketException e) {
            throw new ConfigurationException (e);
        }
    }
    public Collection getIncomingFilters() {
        return incomingFilters;
    }
    public Collection getOutgoingFilters() {
        return outgoingFilters;
    }
    public void setIncomingFilters (Collection filters) {
        incomingFilters = new ArrayList (filters);
    }
    public void setOutgoingFilters (Collection filters) {
        outgoingFilters = new Vector (filters);
    }
    public void setHeader (byte[] header) {
        this.header = header;
    }
    public void setHeader (String header) {
        setHeader (header.getBytes());
    }
    public byte[] getHeader () {
        return header;
    }
    /**
     * @return ISOChannel instance with given name.
     * @throws NameRegistrar.NotFoundException
     * @see NameRegistrar
     */
    public static ISOChannel getChannel (String name)
        throws NameRegistrar.NotFoundException
    {
        return (ISOChannel) NameRegistrar.get ("channel."+name);
    }
   /**
    * Gets the ISOClientSocketFactory (may be null)
    * @see     ISOClientSocketFactory
    * @since 1.3.3
    */
    public ISOClientSocketFactory getSocketFactory() {
        return socketFactory;
    }
   /**
    * Sets the specified Socket Factory to create sockets
    * @param         socketFactory the ISOClientSocketFactory
    * @see           ISOClientSocketFactory
    * @since 1.3.3
    */
    public void setSocketFactory(ISOClientSocketFactory socketFactory) {
        this.socketFactory = socketFactory;
    }
}
