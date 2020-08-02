package com.futeh.posng.message.visa;

import com.futeh.posng.message.BitMapField;
import com.futeh.posng.message.Composite;

import static com.futeh.posng.DataElements.*;

public class Base1 {

    public Composite create() {
        Composite composite = new Composite()
                .set(0, b_num(4).desc("MTI"), new BitMapField(16).desc("bitmap"))
                .set(2, b_num(19, HH).desc("PAN"), b_num(6).desc("processing code"))
                .set(4, b_num(12).desc("acquirer amount"), b_num(12).desc("settlement amount"))
                .set(6, b_num(12).desc("billing amount"))
                .set(7, b_num(7).desc("transmission data time"))
                .set(8, b_num(8).desc("billing fee"), b_num(8).desc("settlement conversion rate"))
                .set(10, b_num(8).desc("billing conversion rate"))
                .set(11, b_num(6).desc("system trace number"))
                .set(12, b_num(6).desc("local time"), b_num(4).desc("local date"))
                .set(14, b_num(4).desc("expiration date"), b_num(4).desc("settlement date"))
                .set(16, b_num(4).desc("conversion date"), b_num(4).desc("capture date"))
                .set(18, b_num(4).desc("mcc"), b_num(3).desc("acquirer institution country code"))
                .set(20, b_num(3).desc("pan extended country code"), b_num(3).desc("forwarding institution country code"))
                .set(22, b_num(4).desc("pos entry mode").rightPadded('0'), b_num(3).desc("card sequence number"))
                .set(24, b_num(3).desc("network international id"), b_num(2).desc("pos condition code"))
                .set(26, b_num(2).desc("pos pin capture mode"), b_num(1).desc("auth id resp len"))
                .set(28, e_amt(9).desc("transaction fee"))
                .set(32, b_num(11, HH).desc("acquiring institution id code"), b_num(11, HH).desc("forwarding institution id code"))
                .set(34, bin(1002, HH).desc("electronic commerce data (TLV format)"))
                .set(35, b_num(37, HH).desc("track 2 data"));

        return composite;
    }
}
