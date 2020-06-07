import static org.junit.Assert.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.junit.Test;
import org.magcode.sem6000.Sem6000MQTT;
import org.magcode.sem6000.send.LoginCommand;
import org.magcode.sem6000.send.SyncTimeCommand;

public class SendMessageTest {
	@Test
	public void testDate() {
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
		LocalDateTime formatDateTime = LocalDateTime.parse("2020-06-07 10:55:22", formatter);
		SyncTimeCommand syncTimeCommand = new SyncTimeCommand(formatDateTime);
		assertEquals("0f0c010016370a070607e4000051ffff", Sem6000MQTT.byteArrayToHex(syncTimeCommand.getMessage()));
	}
	@Test
	public void testLogin() {
		LoginCommand loginCommand = new LoginCommand("1234");
		assertEquals("0f0c170000010203040000000022ffff", Sem6000MQTT.byteArrayToHex(loginCommand.getMessage()));
	}
}