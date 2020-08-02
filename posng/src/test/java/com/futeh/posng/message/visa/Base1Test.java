package com.futeh.posng.message.visa;

import com.futeh.progeny.iso.packager.GenericPackager;
import org.junit.jupiter.api.Test;

import java.io.InputStream;

public class Base1Test {
    @Test
    void basic() throws Exception {
        InputStream in = Base1.class.getResourceAsStream("base1.xml");
        GenericPackager packager = new GenericPackager(in);
    }
}
