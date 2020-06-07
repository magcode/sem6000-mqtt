import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.magcode.sem6000.receive.ResponseType;
import org.magcode.sem6000.receive.SemResponse;
import org.magcode.sem6000.receive.SemResponseParser;
import org.magcode.sem6000.send.Command;

public class ParseNotificationTest {
	@Test
	public void testSynTime() {
		String resp = "0f0401000002ffff";

		SemResponse semResponse = SemResponseParser.parseMessage(Command.hexStringToByteArray(resp));
		assertEquals(ResponseType.synctime, semResponse.getType());

	}

}
