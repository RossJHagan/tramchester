package com.tramchester.integration.mappers;

import com.tramchester.domain.RawJourney;
import com.tramchester.domain.RawStage;
import com.tramchester.domain.RawVehicleStage;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.graph.RouteCalculator;
import org.joda.time.LocalDate;

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;

public class JourneyResponseMapperTest {
    protected RouteCalculator routeCalculator;

    protected String findServiceId(String firstId, String secondId, LocalDate when, int queryTime) throws TramchesterException {
        List<Integer> queryTimes = Arrays.asList(new Integer[]{queryTime});

        Set<RawJourney> found = routeCalculator.calculateRoute(firstId, secondId, queryTimes, new TramServiceDate(when));
        RawJourney rawJourney = found.stream().findFirst().get();
        RawStage rawStage = rawJourney.getStages().get(0);
        assertEquals(RawVehicleStage.class, rawStage.getClass());
        return ((RawVehicleStage)rawStage).getServiceId();
    }
}
