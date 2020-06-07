import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.magcode.sem6000.receive.MeasurementResponse;
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
	public void testMeasure() {
		String resp = "0f11040001002cc8ea0059320000000000006f";
		SemResponse semResponse = SemResponseParser.parseMessage(Command.hexStringToByteArray(resp));
		assertEquals(ResponseType.measure, semResponse.getType());
		MeasurementResponse mRes = (MeasurementResponse) semResponse;
		assertEquals(234, mRes.getVoltage());
		assertEquals(11.464, mRes.getPower(),0.001f);
		
	}
	@Test
	public void testIncompleteDay1() {
		String resp = "0f330a0000000000000000000000000000000000";
		SemResponse semResponse = SemResponseParser.parseMessage(Command.hexStringToByteArray(resp));
		assertEquals(ResponseType.incomplete, semResponse.getType());
	}

	@Test
	public void testIncompleteDay2() {
		String resp = "0f330a0000000000000000000000000000000000";
		resp = resp + "0000000000000000000000000000000000000000";
		SemResponse semResponse = SemResponseParser.parseMessage(Command.hexStringToByteArray(resp));
		assertEquals(ResponseType.incomplete, semResponse.getType());
	}

	@Test
	public void testIncompleteDay3() {
		String resp = "0f330a0000000000000000000000000000000000";
		resp = resp + "0000000000000000000000000000000000000000";
		resp = resp + "0000000000000000000000000bffff";
		SemResponse semResponse = SemResponseParser.parseMessage(Command.hexStringToByteArray(resp));
		assertEquals(ResponseType.dataday, semResponse.getType());
	}
}
