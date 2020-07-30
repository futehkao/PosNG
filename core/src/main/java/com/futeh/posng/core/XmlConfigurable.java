/*
 * Copyright (c) 2004 jPOS.org 
 *
 * See terms of license at http://jpos.org/license.html
 *
 */
package com.futeh.posng.core;

import org.jdom2.Element;

/**
 * Object is Configurable by an Xml Element
 * @author <a href="mailto:apr@cs.com.uy">Alejandro P. Revilla</a>
 * @version $Revision$ $Date$
 * @since jPOS 1.4.9
 */
public interface XmlConfigurable {
   /**
    * @param cfg Configuration element
    * @throws ConfigurationException
    */
    public void setConfiguration (Element e)
        throws ConfigurationException;
}

