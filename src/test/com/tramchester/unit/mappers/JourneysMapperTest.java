package com.tramchester.unit.mappers;

import com.tramchester.domain.RawJourney;
import com.tramchester.domain.TramTime;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.LocationDTO;
import com.tramchester.domain.presentation.DTO.factory.JourneyDTOFactory;
import com.tramchester.domain.presentation.Journey;
import com.tramchester.integration.Stations;
import com.tramchester.mappers.JourneysMapper;
import com.tramchester.mappers.SingleJourneyMapper;
import org.apache.commons.collections4.set.ListOrderedSet;
import org.easymock.EasyMock;
import org.easymock.EasyMockSupport;
import org.joda.time.LocalTime;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class JourneysMapperTest extends EasyMockSupport{

    private JourneyDTOFactory factory;
    private SingleJourneyMapper mapper;

    @Before
    public void beforeEachTestRuns() {
        factory = createMock(JourneyDTOFactory.class);
        mapper = createMock(SingleJourneyMapper.class);
    }

    @Test
    public void shouldMapJourneyThenCreateDTO() {

        JourneysMapper journeysMapper = new JourneysMapper(mapper);

        Set<RawJourney> rawJourneys = new ListOrderedSet<>();
        RawJourney rawJourneyA =new RawJourney(new LinkedList<>(), 3);
        RawJourney rawJourneyB =new RawJourney(new LinkedList<>(), 2);
        RawJourney rawJourneyC =new RawJourney(new LinkedList<>(), 1);

        rawJourneys.add(rawJourneyA);
        rawJourneys.add(rawJourneyB);
        rawJourneys.add(rawJourneyC);

        Journey journeyA = new Journey(new LinkedList<>());
        Journey journeyB = new Journey(new LinkedList<>());

        LocationDTO begin = new LocationDTO(Stations.Altrincham);
        LocationDTO end = new LocationDTO(Stations.Victoria);
        TramTime now = TramTime.now();
        JourneyDTO journeyDTOA = new JourneyDTO(begin, end, new LinkedList<>(), now, now,
                "summaryA", "headingA", false);
        JourneyDTO journeyDTOB = new JourneyDTO(begin, end, new LinkedList<>(), now.plusMinutes(1),
                now.plusMinutes(1),
                "summaryB", "headingB", false);

        EasyMock.expect(mapper.createJourney(rawJourneyA, 42)).andReturn(Optional.of(journeyA));
        EasyMock.expect(factory.build(journeyA)).andReturn(journeyDTOA);

        EasyMock.expect(mapper.createJourney(rawJourneyB, 42)).andReturn(Optional.of(journeyB));
        EasyMock.expect(factory.build(journeyB)).andReturn(journeyDTOB);

        EasyMock.expect(mapper.createJourney(rawJourneyC, 42)).andReturn(Optional.empty());

        replayAll();
        SortedSet<JourneyDTO> results = journeysMapper.map(factory, rawJourneys, 42);
        verifyAll();

        assertEquals(2, results.size());
        assertTrue(results.contains(journeyDTOA));
        assertTrue(results.contains(journeyDTOB));


    }

}
