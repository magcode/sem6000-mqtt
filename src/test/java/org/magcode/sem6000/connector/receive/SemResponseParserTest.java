package org.magcode.sem6000.connector.receive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.magcode.sem6000.connector.receive.SemResponseTestHelper.createIncompleteResponse;
import static org.magcode.sem6000.connector.receive.SemResponseTestHelper.createMeasureResponse;
import static org.magcode.sem6000.connector.receive.SemResponseTestHelper.createSemDayDataResponse;
import static org.magcode.sem6000.connector.receive.SemResponseTestHelper.createSemResponseFor;
import static org.magcode.sem6000.connector.receive.SemResponseTestHelper.createSyncTimeResponse;
import static org.magcode.sem6000.connector.receive.SemResponseTestHelper.createUnknownSemResponse;

import org.junit.jupiter.api.Test;

class SemResponseParserTest {

  @Test
  public void testSynTime() {
    SemResponse semResponse = createSyncTimeResponse();
    assertEquals(ResponseType.synctime, semResponse.getType());
  }

  @Test
  public void wrongOrderDay() {
    SemResponse semResponse = createUnknownSemResponse();
    assertEquals(ResponseType.unknown, semResponse.getType());
  }

  @Test
  public void dataDayTest() {
    SemResponse semResponse = createSemDayDataResponse();
    assertEquals(ResponseType.dataday, semResponse.getType());
    DataDayResponse dataResp = (DataDayResponse) semResponse;
    assertEquals(1638, dataResp.getLast24h());
    assertEquals(1638, dataResp.getToday());
  }

  @Test
  public void testMeasure() {
    SemResponse semResponse = createMeasureResponse();
    assertEquals(ResponseType.measure, semResponse.getType());
    MeasurementResponse mRes = (MeasurementResponse) semResponse;
    assertEquals(234, mRes.getVoltage());
    assertEquals(11.464, mRes.getPower(), 0.001f);
  }

  @Test
  public void testIncompleteDay1() {
    SemResponse semResponse = createSemResponseFor("0f330a0000000000000000000000000000000000");
    assertEquals(ResponseType.incomplete, semResponse.getType());
  }

  @Test
  public void testIncompleteDay2() {
    SemResponse semResponse = createIncompleteResponse();
    assertEquals(ResponseType.incomplete, semResponse.getType());
  }

  @Test
  public void testIncompleteDay3() {
    SemResponse semResponse = createSemResponseFor(
        "0f330a000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000bffff");
    assertEquals(ResponseType.dataday, semResponse.getType());
  }
}