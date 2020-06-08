import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.magcode.sem6000.connector.receive.DataDayResponse;
import org.magcode.sem6000.connector.receive.MeasurementResponse;
import org.magcode.sem6000.connector.receive.ResponseType;
import org.magcode.sem6000.connector.receive.SemResponse;
import org.magcode.sem6000.connector.receive.SemResponseParser;
import org.magcode.sem6000.connector.send.Command;

public class ParseNotificationTest {
	@Test
	public void testSynTime() {
		String resp = "0f0401000002ffff";
		SemResponse semResponse = SemResponseParser.parseMessage(Command.hexStringToByteArray(resp), "dummy");
		assertEquals(ResponseType.synctime, semResponse.getType());
	}

	@Test
	public void testMeasure() {
		String resp = "0f11040001002cc8ea0059320000000000006f";
		SemResponse semResponse = SemResponseParser.parseMessage(Command.hexStringToByteArray(resp), "dummy");
		assertEquals(ResponseType.measure, semResponse.getType());
		MeasurementResponse mRes = (MeasurementResponse) semResponse;
		assertEquals(234, mRes.getVoltage());
		assertEquals(11.464, mRes.getPower(), 0.001f);

	}

	@Test
	public void testIncompleteDay1() {
		String resp = "0f330a0000000000000000000000000000000000";
		SemResponse semResponse = SemResponseParser.parseMessage(Command.hexStringToByteArray(resp), "dummy");
		assertEquals(ResponseType.incomplete, semResponse.getType());
	}

	@Test
	public void testIncompleteDay2() {
		String resp = "0f330a0000000000000000000000000000000000";
		resp = resp + "0000000000000000000000000000000000000000";
		SemResponse semResponse = SemResponseParser.parseMessage(Command.hexStringToByteArray(resp), "dummy");
		assertEquals(ResponseType.incomplete, semResponse.getType());
	}

	@Test
	public void testIncompleteDay3() {
		String resp = "0f330a0000000000000000000000000000000000";
		resp = resp + "0000000000000000000000000000000000000000";
		resp = resp + "0000000000000000000000000bffff";
		SemResponse semResponse = SemResponseParser.parseMessage(Command.hexStringToByteArray(resp), "dummy");
		assertEquals(ResponseType.dataday, semResponse.getType());
	}

	@Test
	public void dataDayTest() {
		String resp = "0f330a0000000000000000000000000000000005000b000b000000000000000000000000000000000000000000000001000d00063affff";
		SemResponse semResponse = SemResponseParser.parseMessage(Command.hexStringToByteArray(resp), "dummy");
		assertEquals(ResponseType.dataday, semResponse.getType());
		DataDayResponse dataResp = (DataDayResponse) semResponse;
		assertEquals(47, dataResp.getLast24h());
		assertEquals(20, dataResp.getToday());
	}

}
