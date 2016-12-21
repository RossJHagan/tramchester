package com.tramchester.resources;


import com.tramchester.App;
import com.tramchester.IntegrationClient;
import com.tramchester.IntegrationTestRun;
import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.domain.FeedInfo;
import org.joda.time.LocalDate;
import org.junit.ClassRule;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Optional;

import static org.junit.Assert.assertEquals;

public class FeedInfoResourceTest {
    public static LocalDate validFrom = new LocalDate(2016, 12, 20);
    public static LocalDate validUntil = new LocalDate(2017, 2, 20);

    @ClassRule
    public static IntegrationTestRun testRule = new IntegrationTestRun(App.class, new IntegrationTramTestConfig());

    @Test
    public void shouldGetFeedinfoCorrectly() {
        String endPoint = "feedinfo";

        Response responce = IntegrationClient.getResponse(testRule, endPoint, Optional.empty());

        FeedInfo result = responce.readEntity(FeedInfo.class);

        assertEquals("Transport for Greater Manchester", result.getPublisherName());
        assertEquals("http://www.tfgm.com", result.getPublisherUrl());
        assertEquals("Europe/London", result.getTimezone());
        assertEquals("en", result.getLang());
        assertEquals(validFrom.toString("YYYYMMdd"), result.getVersion());
        assertEquals(validFrom, result.validFrom());
        assertEquals(validUntil, result.validUntil());
    }

}
