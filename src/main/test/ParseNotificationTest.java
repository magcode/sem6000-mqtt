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

	@Test
	public void testIncomplete1() {
		String resp = "0f7b0b0000000000000000000000000000000000";
		SemResponse semResponse = SemResponseParser.parseMessage(Command.hexStringToByteArray(resp));
		assertEquals(ResponseType.incomplete, semResponse.getType());
	}

	@Test
	public void testIncomplete2() {
		String resp = "0f7b0b0000000000000000000000000000000000";
		resp = resp + "0000000000000000000000000000000000000000";
		SemResponse semResponse = SemResponseParser.parseMessage(Command.hexStringToByteArray(resp));
		assertEquals(ResponseType.incomplete, semResponse.getType());
	}

	@Test
	public void testIncomplete3() {
		String resp = "0f7b0b0000000000000000000000000000000000";
		resp = resp + "0000000000000000000000000000000000000000";
		resp = resp + "0000000000000000000000000000000000000000";
		SemResponse semResponse = SemResponseParser.parseMessage(Command.hexStringToByteArray(resp));
		assertEquals(ResponseType.incomplete, semResponse.getType());
	}

	@Test
	public void testIncomplete4() {
		String resp = "0f7b0b0000000000000000000000000000000000";
		resp = resp + "0000000000000000000000000000000000000000";
		resp = resp + "0000000000000000000000000000000000000000";
		resp = resp + "0f11040000000000ea00003200000000000021";
		SemResponse semResponse = SemResponseParser.parseMessage(Command.hexStringToByteArray(resp));
		assertEquals(ResponseType.incomplete, semResponse.getType());
	}
}
