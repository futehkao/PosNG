package com.futeh.posng.message;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static com.futeh.posng.DataElements.*;
import static com.futeh.posng.DataElements.H;
import static org.junit.jupiter.api.Assertions.assertEquals;

@Tag("posng")
public class CompositeTest {
    @Test
    void padded() throws IOException {
        Composite composite = new Composite()
                .dataLength(HH)
                .set(1, e_char(1))
                .set(2, e_char(2))
                .set(3, e_char(4));
        Message msg = new Message(composite)
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

    @Test
    void nested() throws IOException {
        Composite composite = new Composite()
                .set(0, b_num(4).desc("MTI"))
                .set(1, new BitMapField(16).desc("bitmap"))
                .set(2, b_num(19, H).desc("PAN"))
                .set(3, new Composite().dataLength(H).desc("additional data")
                        .set(1, e_char(1).desc("response source/reason code"))
                        .set(2, e_char(1).desc("address verification result code"))
                );
        Message msg = new Message(composite);
        msg.set(0, "0100")
                .set(2, "1234567890123456789")
                .set(3, new Message()
                    .set(1, "a")
                    .set(2, "b"));
        byte[] bytes = composite.write(msg);
        Message msg2 = composite.read(bytes);
    }
}
