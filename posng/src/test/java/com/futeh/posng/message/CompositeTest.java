package com.futeh.posng.message;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.futeh.posng.DataElements.HH;
import static com.futeh.posng.DataElements.e_char;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class CompositeTest {
    @Test
    void padded() throws IOException {
        Composite composite = new Composite()
                .dataLength(HH)
                .set(1, e_char(1))
                .set(2, e_char(2))
                .set(3, e_char(4));
        Message msg = new Message()
                .set(1, "1");
        byte[] bytes = composite.write(msg);
        Message msg2 = composite.read(bytes);
        assertEquals((Object) msg.get(1), msg2.get(1));

        msg.unset(1).set(3, "2");
        bytes = composite.write(msg);
        msg2 = composite.read(bytes);
        assertEquals("  ", msg2.get(2));
        assertEquals(msg2.get(3), "2   ");
    }
}
