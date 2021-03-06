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

package com.futeh.progeny.util;

import java.util.*;

/**
 * Peer class Logger forwards LogEvents generated by LogSources 
 * to LogListeners.
 * <br>
 * This little <a href="/doc/LoggerGuide.html">tutorial</a>
 * give you additional information on how to extend the jPOS's
 * Logger subsystem.
 *
 * @author apr@cs.com.uy
 * @version $Id$
 * @see LogEvent
 * @see LogSource
 * @see LogListener
 * @see Loggeable
 * @see SimpleLogListener
 * @see RotateLogListener
 */
public class Logger {
    String name;
    List listeners ;

    public Logger () {
        super();
        listeners = Collections.synchronizedList (new ArrayList<>());
        name = "";
    }
    public void addListener (LogListener l) {
        synchronized (listeners) {
            listeners.add (l);
        }
    }
    public void removeListener (LogListener l) {
        synchronized (listeners) {
            listeners.remove (l);
        }
    }
    public void removeAllListeners () {
        synchronized (listeners) {
            Iterator i = listeners.iterator();
            while (i.hasNext()) {
                LogListener l = ((LogListener) i.next());
                if (l instanceof Destroyable) {
                    ((Destroyable) l).destroy ();
                }
            }
            listeners.clear ();
        }
    }
    public static void log (LogEvent evt) {
        Logger l = null;
        LogSource source = evt.getSource();
        if (source != null)
            l = source.getLogger();
        if (l != null && l.hasListeners ()) {
            synchronized (l.listeners) {
                Iterator i = l.listeners.iterator();
                while (i.hasNext() && evt != null) 
                    evt = ((LogListener) i.next()).log (evt);
            }
        }
    }
    /**
     * associates this Logger with a name using NameRegistrar
     * @param name name to register
     * @see NameRegistrar
     */
    public void setName (String name) {
        this.name = name;
        NameRegistrar.register ("logger."+name, this);
    }
    /**
     * destroy logger
     */
    public void destroy () {
        NameRegistrar.unregister ("logger."+name);
        removeAllListeners ();
    }
    /**
     * @return logger instance with given name. Creates one if necessary
     * @see NameRegistrar
     */
    public synchronized static Logger getLogger (String name) {
        Logger l;
        try {
            l = (Logger) NameRegistrar.get ("logger."+name);
        } catch (NameRegistrar.NotFoundException e) {
            l = new Logger();
            l.setName (name);
        }
        return l;
    }
    /**
     * @return this logger's name ("" if no name was set)
     */
    public String getName() {
        return this.name;
    }
    /**
     * Used by heavy used methods to avoid LogEvent creation 
     * @return true if Logger has associated LogListsners
     */
    public boolean hasListeners() {
        synchronized (listeners) {
            return listeners.size() > 0;
        }
    }
}
