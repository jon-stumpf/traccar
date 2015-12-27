package org.traccar.protocol;

import org.traccar.helper.TestDataManager;
import static org.traccar.helper.DecoderVerifier.verify;
import static org.junit.Assert.assertNull;
import org.junit.Test;
import java.net.InetSocketAddress;

public class Esky620ProtocolDecoderTest {

    @Test
    public void testDecode() throws Exception {

        Esky620ProtocolDecoder decoder = new Esky620ProtocolDecoder(new TestDataManager(), null, null);

        //////////////
        // Error cases

        // String too short
        assertNull(decoder.decode(null, null, "EL;0"));

        //////////////
        // Valid cases

        // Login Messages (they return null because they don't report position)
        assertNull(decoder.decode(null, null,
                "EL;1;123456789012345;150408015203;"));

        assertNull(decoder.decode(null, null,
                "EL;1;123456789012345;150408015207;"));

        assertNull(decoder.decode(null, null,
                "EL;1;123456789012345;150408015212;"));

        // Report Messages
        verify(decoder.decode(null, null,
	       "EO;1;123456789012345;RG;4+150407185854+44.58039+-74.71671+0.24+0+4241+1"));

        verify(decoder.decode(null, null,
	       "EO;1;123456789012345;RG;4+150407193657+44.58023+-74.71762+0.93+353+4301+1"));

        verify(decoder.decode(null, null,
	       "EO;1;123456789012345;RG;4+150407194144+44.57993+-74.71756+0.77+169+4241+1"));

        verify(decoder.decode(null, null,
	       "EO;1;123456789012345;RG;5+150407202222+44.57999+-74.71773+0.73+344+4210+1"));

        verify(decoder.decode(null, null,
	       "EO;1;123456789012345;RG;5+150407212257+44.58000+-74.71766+0.66+70+4159+1"));
    }

}
