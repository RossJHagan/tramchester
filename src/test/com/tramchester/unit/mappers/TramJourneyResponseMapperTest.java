package com.tramchester.unit.mappers;

import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StageDTO;
import com.tramchester.domain.presentation.DTO.factory.JourneyDTOFactory;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ServiceTime;
import com.tramchester.domain.presentation.TransportStage;
import com.tramchester.mappers.TramJourneyResponseMapper;
import com.tramchester.repository.TransportDataFromFiles;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TramJourneyResponseMapperTest extends EasyMockSupport {

    private static final int AM8 = 8 * 60;
    private TramJourneyResponseMapper mapper;
    private TransportDataFromFiles transportData;
    private List<RawStage> stages;
    private Station stationA;
    private Station stationB;
    private Station stationC;
    private Platform platformA;
    private Platform platformB;

    @Before
    public void beforeEachTestRuns() {
        stages = new LinkedList<>();

        stationA = new Station("stationA", "area", "nameA", new LatLong(-2, -1), false);
        stationB = new Station("stationB", "area", "nameA", new LatLong(-3, 1), false);
        stationC = new Station("stationC", "area", "nameA", new LatLong(-4, 2), false);

        platformA = new Platform(stationA.getId() + "1", "platformA 1");
        platformB = new Platform(stationB.getId() + "1", "platformA 2");

        transportData = createMock(TransportDataFromFiles.class);
        mapper = new TramJourneyResponseMapper(transportData);
    }

    @Test
    public void testSimpleMappingOfJourney() throws TramchesterException {

        RawJourney journey = createSimpleRawJourney(7, 9, AM8);

        EasyMock.expect(transportData.getStation("stationA")).andStubReturn(Optional.of(stationA));
        EasyMock.expect(transportData.getStation("stationB")).andStubReturn(Optional.of(stationB));
        EasyMock.expect(transportData.getStation("stationC")).andStubReturn(Optional.of(stationC));

        Optional<ServiceTime> timesLeg1 = Optional.of(new ServiceTime(TramTime.create(8,2), TramTime.create(8,9), "svcId",
                "headSign", "tripIdA"));

        Optional<ServiceTime> timesLeg2 = Optional.of(new ServiceTime(TramTime.create(8,9), TramTime.create(8,18),
                "svcId", "headSign", "tripIdA"));

        EasyMock.expect(transportData.getFirstServiceTime("svcId", stationA, stationB, new TimeWindow(AM8, 30))).andReturn(timesLeg1);
        EasyMock.expect(transportData.getFirstServiceTime("svcId", stationB, stationC, new TimeWindow(AM8+9, 30))).
                andReturn(timesLeg2);

        replayAll();
        Optional<Journey> result = mapper.createJourney(journey, 30);
        verifyAll();

        assertTrue(result.isPresent());
        Journey found = result.get();

        TransportStage stageFirst = found.getStages().get(0);
        TransportStage stageSecond = found.getStages().get(1);
        assertEquals(7, stageFirst.getDuration());
        assertEquals(9, stageSecond.getDuration());
        assertEquals(TramTime.create(8,18),stageSecond.getExpectedArrivalTime());
        assertEquals(TramTime.create(8,2),stageFirst.getFirstDepartureTime());
    }

    @Test
    public void testSimpleMappingOfJourneyUseEarliestArrive() throws TramchesterException {

        RawJourney journey = createSimpleRawJourney(4, 9, AM8);

        EasyMock.expect(transportData.getStation("stationA")).andStubReturn(Optional.of(stationA));
        EasyMock.expect(transportData.getStation("stationB")).andStubReturn(Optional.of(stationB));
        EasyMock.expect(transportData.getStation("stationC")).andStubReturn(Optional.of(stationC));

        Optional<ServiceTime> timesLeg1 = Optional.of(new ServiceTime(TramTime.create(8,3), TramTime.create(8,7),
                "svcId", "headSign", "tripIdA"));

        Optional<ServiceTime> timesLeg2 = Optional.of(new ServiceTime(TramTime.create(8,7), TramTime.create(8,16),
                "svcId", "headSign", "tripIdA"));

        EasyMock.expect(transportData.getFirstServiceTime("svcId", stationA, stationB, new TimeWindow(AM8, 30))).andReturn(timesLeg1);
        EasyMock.expect(transportData.getFirstServiceTime("svcId", stationB, stationC, new TimeWindow(AM8+7, 30))).andReturn(timesLeg2);

        replayAll();
        Optional<Journey> result = mapper.createJourney(journey, 30);
        verifyAll();
        assertTrue(result.isPresent());

        Journey found = result.get();

        TransportStage stageFirst = found.getStages().get(0);
        TransportStage stageSecond = found.getStages().get(1);
        assertEquals(4, stageFirst.getDuration());
        assertEquals(9, stageSecond.getDuration());

        assertEquals(TramTime.create(8,16),stageSecond.getExpectedArrivalTime());
        assertEquals(TramTime.create(8,3),stageFirst.getFirstDepartureTime());

        assertEquals("Board bus at", stageFirst.getPrompt());
        assertEquals("Change bus at", stageSecond.getPrompt());
    }

    private RawJourney createSimpleRawJourney(int costA, int costB, int queryTime) {
        RawVehicleStage rawTravelStage1 = new RawVehicleStage(stationA, "routeNameA", TransportMode.Bus, "routeIdA").
                setLastStation(stationB).setServiceId("svcId").setCost(costA);
        rawTravelStage1.setPlatform(platformA);
        RawVehicleStage rawTravelStage2 = new RawVehicleStage(stationB, "routeNameA", TransportMode.Bus, "routeIdA").
                setLastStation(stationC).setServiceId("svcId").setCost(costB);
        rawTravelStage2.setPlatform(platformB);
        stages.add(rawTravelStage1);
        stages.add(rawTravelStage2);

        return new RawJourney(stages, queryTime);
    }

}
