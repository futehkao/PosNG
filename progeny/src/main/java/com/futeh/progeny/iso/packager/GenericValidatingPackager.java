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

package com.futeh.progeny.iso.packager;

import java.io.InputStream;
import java.util.*;


import com.futeh.progeny.core.ConfigurationException;
import com.futeh.progeny.core.SimpleConfiguration;
import com.futeh.progeny.util.LogEvent;
import com.futeh.progeny.util.Logger;
import com.futeh.progeny.iso.ISOBasePackager;
import com.futeh.progeny.iso.ISOBaseValidator;
import com.futeh.progeny.iso.ISOComponent;
import com.futeh.progeny.iso.ISOException;
import com.futeh.progeny.iso.ISOFieldPackager;
import com.futeh.progeny.iso.ISOFieldValidator;
import com.futeh.progeny.iso.ISOMsg;
import com.futeh.progeny.iso.ISOMsgFieldPackager;
import com.futeh.progeny.iso.ISOMsgFieldValidator;
import com.futeh.progeny.iso.ISOValidator;
import com.futeh.progeny.iso.validator.ISOVException;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;
import org.xml.sax.helpers.XMLReaderFactory;


/**
 * Generic Packager that configure validators too.
 * <p>Title: jPOS</p>
 * <p>Description: Java Framework for Financial Systems</p>
 * <p>Copyright: Copyright (c) 2000 jPOS.org.  All rights reserved.</p>
 * <p>Company: www.jPOS.org</p>
 * @author Jose Eduardo Leon
 * @version 1.0
 */
public class GenericValidatingPackager extends GenericPackager implements ISOValidator {

    public GenericValidatingPackager(  ) throws ISOException{
        super();
    }
    public GenericValidatingPackager( String fileName ) throws ISOException {
        super( fileName );
    }
    public GenericValidatingPackager (InputStream stream) throws ISOException {
        super (stream);
    }

    /**
     * Convert the ISOFieldPackagers in the HashMap
     * to an array of ISOFieldPackagers
     */
    private ISOFieldPackager[] makeFieldArray(Map tab)
    {
        int maxField = 0;

        // First find the largest field number in the HashMap
        for (Object k : tab.keySet())
        {
            int n = ((Integer)k).intValue();
            if (n > maxField) maxField = n;
        }

        // Create the array
        ISOFieldPackager fld[] = new ISOFieldPackager[maxField+1];

        // Populate it
        for (Object k : tab.keySet())
        {
            Integer key = (Integer) k;
            fld[key.intValue()] = (ISOFieldPackager)tab.get(key);
        }
        return fld;
    }

    /**
     * It define GenericValidatorContentHandler like handler.
     */
    public void readFile(String filename) throws ISOException {
        try {
            XMLReader reader = XMLReaderFactory.createXMLReader(
                    System.getProperty( "sax.parser",
                    "org.apache.xerces.parsers.SAXParser"));
            reader.setFeature ("http://xml.org/sax/features/validation", true);
            GenericValidatorContentHandler handler = new GenericValidatorContentHandler();
            reader.setContentHandler(handler);
            reader.setErrorHandler(handler);
            reader.parse(filename);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            throw new ISOException(e);
        }
    }

    protected void setGenericPackagerParams ( Attributes atts ) {
        String maxField  = atts.getValue( "maxValidField" );
        String emitBmap  = atts.getValue( "emitBitmap" );
        String bmapfield = atts.getValue( "bitmapField" );
        if ( maxField != null )
            maxValidField = Integer.parseInt( maxField );
        if ( emitBmap != null )
            emitBitmap = Boolean.valueOf( emitBmap ).booleanValue();
        if ( bmapfield != null )
            bitmapField = Integer.parseInt( bmapfield );
    }

    public void setMsgValidator( ISOBaseValidator[] msgVlds ){
        this.mvlds = msgVlds;
    }

    public void setFieldValidator( ISOFieldValidator[] fvlds ){
        this.fvlds = fvlds;
    }

    public ISOComponent validate(ISOComponent m) throws ISOException {
        LogEvent evt = new LogEvent( this, "validate" );
        try {
            ISOComponent c;
            Map<Integer, Object> fields = ((ISOMsg)m).getChildren();
            int len = 0;
            int first = 0;
            byte[] b;
            /** Field  validations **/
            for (int i=0; i < fvlds.length; i++) {
                if ( fvlds[i] != null && (c=(ISOComponent) fields.get (new Integer ( ((ISOFieldValidator)fvlds[i]).getFieldId() ))) != null ){
                    try {
                        m.set( fvlds[i].validate( c ) );
                    } catch ( ISOVException e ) {
                        if ( !e.treated() ) {
                            m.set( e.getErrComponent() );
                            e.setTreated( true );
                        }
                        evt.addMessage( "Component Validation Error." );
                        throw e;
                    }
                }
            }
            /** msg validations **/
            try {
                if ( mvlds != null ){
                    for (int i = 0; i < this.mvlds.length; i++) {
                        if ( mvlds[i] != null )
                            m = mvlds[i].validate( m );
                    }
                }
            }
            catch (ISOVException ex) {
                evt.addMessage( "Component Validation Error." );
                throw ex;
            }
            return m;
        }
        finally {
            Logger.log( evt );
        }
    }

/*  Values copied from ISOBasePackager
These can be changes using attributes on the isopackager node */
    protected  int maxValidField=128;
    protected boolean emitBitmap=true;
    protected int bitmapField=1;
    /** FieldValidator array. **/
    protected ISOValidator[] fvlds;
    /** MsgValidator array **/
    protected ISOBaseValidator[] mvlds;
    /** incr used to put validators in the same hashtable of
     *  fieldpackagers. packagers will stay on index 1, 2, 3...
     *  and validators in inc+1, inc+2, inc+3,... **/
    final int inc = 500;


    public class GenericValidatorContentHandler extends DefaultHandler {
        public void startDocument(){
            fieldStack = new Stack();
            validatorStack = new Stack();
        }

        public void endDocument() throws SAXException {
            if ( !fieldStack.isEmpty() )
                throw new SAXException ( "Format error in XML Field Description File" );
        }

        public void startElement( String namespaceURI, String localName, String qName, Attributes atts )
                throws SAXException {
            try {
                if ( localName.equals( "isopackager" ) ) {
                    // Stick a new HashMap on stack to collect the fields
                    fieldStack.push( new HashMap<>() );

                    /** used to insert msg-level validators **/
                    Map hash = new HashMap();
                    hash.put( new Integer( VALIDATOR_INDEX ), new ArrayList<>() );

                    validatorStack.push( hash );
                    setGenericPackagerParams ( atts );
                }
                if (localName.equals("isofield")){
                    /** getID global for isofieldvalidator **/
                    fldID = atts.getValue("id");
                    String type = atts.getValue("class");
                    String name = atts.getValue("name");
                    String size = atts.getValue("length");
                    String pad  = atts.getValue("pad");
                    Class c = Class.forName(type);
                    ISOFieldPackager f;
                    f = (ISOFieldPackager) c.newInstance();
                    f.setDescription(name);
                    f.setLength(Integer.parseInt(size));
                    f.setPad(new Boolean(pad).booleanValue());
                    // Insert this new isofield into the HashMap
                    // on the top of the stack using the fieldID as the key
                    Map ht = (Map) fieldStack.peek();
                    ht.put(new Integer(fldID), f);
                }
                if ( localName.equals( "isofieldvalidator" ) ){
                    String type = atts.getValue( "class" );
                    String breakOnError = atts.getValue( "break-on-error" );
                    String minLen = atts.getValue( "minlen" );
                    String maxLen = atts.getValue( "maxlen" );
                    Class c = Class.forName( type );
                    ISOFieldValidator v = (ISOFieldValidator)c.newInstance();
                    if ( breakOnError != null ) v.setBreakOnError( Boolean.valueOf( breakOnError ).booleanValue() );
                    if ( minLen != null ) v.setMinLength( Integer.parseInt( minLen ) );
                    if ( maxLen != null ) v.setMaxLength( Integer.parseInt( maxLen ) );
                    v.setFieldId( Integer.parseInt(fldID) );
                    /** insert validator on stack waiting for properties **/
                    validatorStack.push( v );
                    validatorStack.push( new Properties() );
                }
                if ( localName.equals( "property" ) ){
                    ((Properties)validatorStack.peek()).setProperty(
                            atts.getValue( "name" ),
                            atts.getValue( "value" ) );
                }
                if ( localName.equals( "isovalidator" ) ){
                    String type = atts.getValue( "class" );
                    String breakOnError = atts.getValue( "break-on-error" );
                    Class c = Class.forName( type );
                    ISOBaseValidator v = (ISOBaseValidator)c.newInstance();
                    if ( breakOnError != null ) v.setBreakOnError( Boolean.valueOf( breakOnError ).booleanValue() );
                    /** insert validator on stack waiting for properties **/
                    validatorStack.push( v );
                    validatorStack.push( new Properties() );
                }
                if ( localName.equals("isofieldpackager") ) {
                    String id   = atts.getValue("id");
                    String type = atts.getValue("class");
                    String name = atts.getValue("name");
                    String size = atts.getValue("length");
                    String pad  = atts.getValue("pad");
/*
For a isofield packager node push the following fields
onto the stack.
1) an Integer indicating the field ID
2) an instance of the specified ISOFieldPackager class
3) an instance of the specified ISOBasePackager (msgPackager) class
4) a HashMap to collect the subfields
*/
                    String packager = atts.getValue("packager");
                    fieldStack.push(new Integer(id));
                    ISOFieldPackager f;
                    f = (ISOFieldPackager) Class.forName(type).newInstance();
                    f.setDescription(name);
                    f.setLength(Integer.parseInt(size));
                    f.setPad(new Boolean(pad).booleanValue());
                    fieldStack.push(f);
                    ISOBasePackager p;
                    p = (ISOBasePackager) Class.forName(packager).newInstance();
                    if (p instanceof GenericValidatingPackager){
                        GenericValidatingPackager gp = (GenericValidatingPackager) p;
                        gp.setGenericPackagerParams (atts);
                    }
                    fieldStack.push(p);
                    String validator = atts.getValue( "validator" );
                    ISOBaseValidatingPackager v;
                    v = (ISOBaseValidatingPackager) Class.forName(validator).newInstance();
                    validatorStack.push( v );
                    HashMap hash = new HashMap();
                    hash.put( new Integer( VALIDATOR_INDEX ), new ArrayList() );
                    validatorStack.push( hash );
                    fieldStack.push( new HashMap() );
                }
            } catch (Exception ex){
                throw new SAXException(ex);
            }
        }

        /**
         * Convert the ISOFieldPackagers in the HashMap
         * to an array of ISOFieldPackagers
         */
        private ISOFieldPackager[] makeFieldPackArray(Map tab){
            int maxField = 0;
            // First find the largest field number in the HashMap
            for (Object k : tab.keySet()){
                int n = ((Integer)k).intValue();
                if (n > maxField) maxField = n;
            }
            // Create the array
            ISOFieldPackager fld[] = new ISOFieldPackager[maxField+1];
            // Populate it
            for (Object k : tab.keySet()){
                Integer key = (Integer) k;
                fld[key.intValue()] = (ISOFieldPackager)tab.get(key);
            }
            return fld;
        }

        public void endElement(String namespaceURI, String localName, String qName) {
            if (localName.equals("isopackager")){
                Map tab  = (Map)fieldStack.pop();
                setFieldPackager( makeFieldPackArray(tab) );
                tab = (Map)validatorStack.pop();
                setFieldValidator ( makeFieldValidatorArray( tab ));
                setMsgValidator( makeMsgValidatorArray( tab ) );
            }
            if ( localName.equals( "isofieldvalidator" ) ){
                /** pop properties **/
                Properties p = (Properties)validatorStack.pop();
                SimpleConfiguration cfg = null;
                if ( !p.entrySet().isEmpty() )
                    cfg = new SimpleConfiguration( p );
                /** pop validator and add it to the hash **/
                ISOFieldValidator f = (ISOFieldValidator)validatorStack.pop();
                if ( cfg != null ){
                    try {
                        f.setConfiguration( cfg );
                    }
                    catch (ConfigurationException ex) {
                        ex.printStackTrace(  );
                    }
                }
                ((Map)validatorStack.peek()).put( new Integer(fldID), f );
            }
            if ( localName.equals( "isovalidator" ) ){
                /** pop properties **/
                Properties p = (Properties)validatorStack.pop();
                SimpleConfiguration cfg = null;
                if ( !p.entrySet().isEmpty() )
                    cfg = new SimpleConfiguration( p );
                /** pop validator and add it to the hash **/
                ISOBaseValidator v = (ISOBaseValidator)validatorStack.pop();
                if ( cfg != null ){
                    try {
                        v.setConfiguration( cfg );
                    }
                    catch (ConfigurationException ex) {
                        ex.printStackTrace(  );
                    }
                }
                /** add validator to the has **/
                ((List)((Map)validatorStack.peek()).get( new Integer(VALIDATOR_INDEX) )).add( v );
            }
            if (localName.equals("isofieldpackager")){
                // Pop the 4 entries off the stack in the correct order
                Map tab = (Map)fieldStack.pop();
                ISOBasePackager msgPackager = (ISOBasePackager) fieldStack.pop();
                msgPackager.setFieldPackager (makeFieldArray(tab));
                msgPackager.setLogger (getLogger(), "Generic Packager");
                ISOFieldPackager fieldPackager = (ISOFieldPackager) fieldStack.pop();
                Integer fno = (Integer) fieldStack.pop();
                // Create the ISOMsgField packager with the retrieved msg and field Packagers
                ISOMsgFieldPackager mfp =
                        new ISOMsgFieldPackager(fieldPackager, msgPackager);

                // Add the newly created ISOMsgField packager to the
                // lower level field stack
                tab=(Map)fieldStack.peek();
                tab.put(fno, mfp);
                Map val = (Map)validatorStack.pop();
                ISOBaseValidatingPackager v = (ISOBaseValidatingPackager) validatorStack.pop();
                v.setFieldValidator( makeFieldValidatorArray ( val ) );
                v.setMsgValidator( makeMsgValidatorArray ( val ) );
                ISOMsgFieldValidator mfv = new ISOMsgFieldValidator ( fieldPackager.getDescription(), v );
                mfv.setFieldId( fno.intValue() );
                v.setLogger (getLogger(), "Generic validating Packager");
                tab=(Map)validatorStack.peek();
                tab.put(fno, mfv);
            }
        }

        ISOFieldValidator[] makeFieldValidatorArray ( Map tab ){
            int maxField = 0;
            // Create the array
            ISOFieldValidator fvlds[] = new ISOFieldValidator[tab.keySet().size()];
            // Populate it
            int ind = 0;
            for (Object k : tab.keySet()){
                Integer key = (Integer) k;
                if ( key.intValue() != VALIDATOR_INDEX ){
                    fvlds[ind] = (ISOFieldValidator)tab.get(key);
                    ind ++;
                }
            }
            return fvlds;
        }

        ISOBaseValidator[] makeMsgValidatorArray ( Map tab ){
            // First find the count
            List v = (List)tab.get( new Integer( VALIDATOR_INDEX ) );
            if ( v==null || v.size() <= 0 ) return null;
            ISOBaseValidator[] r = new ISOBaseValidator[v.size()];
            for (int i = 0; i < v.size(); i++)
                r[i] = (ISOBaseValidator)v.get( i );
            return r;
        }

        // ErrorHandler Methods
        public void error (SAXParseException ex) throws SAXException
        {
            throw ex;
        }

        public void fatalError (SAXParseException ex) throws SAXException
        {
            throw ex;
        }

        final int VALIDATOR_INDEX = -3 ;
        private Stack fieldStack, validatorStack;
        private String fldID;

    }
}
