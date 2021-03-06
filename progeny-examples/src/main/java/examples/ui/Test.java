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

package examples.ui;

import org.jdom2.Document;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import com.futeh.progeny.ui.UI;
import com.futeh.progeny.util.Log;
import com.futeh.progeny.util.Logger;
import com.futeh.progeny.util.SimpleLogListener;

import java.io.File;
import java.io.FileWriter;

public class Test {
    public static int usage() {
        System.out.println("Usage: bin/example ui hello.xml");
        return 0;
    }

    public static void main(String[] args) throws Exception {
        if (args.length == 0)
            System.exit(usage());
        Logger logger = Logger.getLogger("Test");
        logger.addListener(new SimpleLogListener(System.out));
        Document doc = read(new File("src/examples/ui/" + args[0]));
        UI ui = new UI();
        ui.setConfig(doc.getRootElement());
        ui.setLog(new Log(logger, "examples-ui"));
        ui.configure();
        Log log = new Log(logger, "test");
        for (int i = 0; i < 10; i++)
            log.info("Test message " + i);
    }

    public static void write(Document doc, File f)
            throws Exception {
        XMLOutputter out = new XMLOutputter(Format.getPrettyFormat());
        FileWriter writer = new FileWriter(f);
        out.output(doc, writer);
        writer.close();
    }

    public static Document read(File f)
            throws Exception {
        SAXBuilder builder = new SAXBuilder();
        Document doc = builder.build(f);
        return doc;
    }

}

