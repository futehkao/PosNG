/*
 * Copyright (c) 2000 jPOS.org. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *  1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *  2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *  3. The end-user documentation included with the redistribution, if any,
 * must include the following acknowledgment: "This product includes software
 * developed by the jPOS project (http://www.jpos.org/)". Alternately, this
 * acknowledgment may appear in the software itself, if and wherever such
 * third-party acknowledgments normally appear.
 *  4. The names "jPOS" and "jPOS.org" must not be used to endorse or promote
 * products derived from this software without prior written permission. For
 * written permission, please contact license@jpos.org.
 *  5. Products derived from this software may not be called "jPOS", nor may
 * "jPOS" appear in their name, without prior written permission of the jPOS
 * project.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE JPOS
 * PROJECT OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO,
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS;
 * OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR
 * OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the jPOS Project. For more information please see
 * <http://www.jpos.org/> .
 */

package iso;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import com.futeh.progeny.iso.RightPadder;

/**
 * @author joconnor
 */
@Tag("progeny")
public class RightPadderTest  {
    private RightPadder padder;

    @BeforeEach
    protected void setUp() {
        padder = new RightPadder('0');
    }
    
    @Test
    public void tePaddingNeeded() throws Exception {
        assertEquals("123000", padder.pad("123", 6));
    }

    @Test
    public void teNoPaddingNeeded() throws Exception {
        assertEquals("123", padder.pad("123", 3));
    }

    @Test
    public void tePadLengthTooShort() throws Exception {
        try {
            padder.pad("123", 2);
            fail("Padding a bigger string into a smaller buffer should throw an exception");
        } catch (Exception asIExpected) {
        }
    }

    @Test
    public void teUnpad1() throws Exception {
        assertEquals("123", padder.unpad("123000"));
    }

    @Test
    public void teUnpad2() throws Exception {
        assertEquals("123", padder.unpad("123"));
    }

    @Test
    public void teUnpad3() throws Exception {
        assertEquals("1203", padder.unpad("1203000"));
    }

    @Test
    public void teUnpadAllPadding() throws Exception {
        assertEquals("", padder.unpad("000"));
    }

    @Test
    public void teReversability() throws Exception {
        String origin = "Abc";
        assertEquals(origin, padder.unpad(padder.pad(origin, 6)));
    }
}
