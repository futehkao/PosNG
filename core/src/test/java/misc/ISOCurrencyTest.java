package misc;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import com.futeh.posng.iso.ISOCurrency;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
* 
* @author cdaszenies
*/
public class ISOCurrencyTest {

    @Test
    public void testConvertToIsoMsg() {
        assertEquals("000000003848",
            ISOCurrency.convertToIsoMsg(38.48,"EUR"));
    } 
}

