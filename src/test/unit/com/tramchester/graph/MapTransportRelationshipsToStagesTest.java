package com.tramchester.graph;


import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TransportStage;
import com.tramchester.domain.presentation.VehicleStageWithTiming;
import com.tramchester.graph.Nodes.RouteStationNode;
import com.tramchester.graph.Nodes.StationNode;
import com.tramchester.graph.Relationships.BoardRelationship;
import com.tramchester.graph.Relationships.GoesToRelationship;
import com.tramchester.graph.Relationships.TramGoesToRelationship;
import com.tramchester.graph.Relationships.TransportRelationship;
import com.tramchester.repository.StationRepository;
import com.tramchester.resources.RouteCodeToClassMapper;
import org.easymock.EasyMockSupport;
import org.joda.time.LocalDate;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.LinkedList;
import java.util.List;

import static org.junit.Assert.assertNotNull;

public class MapTransportRelationshipsToStagesTest extends EasyMockSupport {
    private MapTransportRelationshipsToStages mapper;
    private StationRepository stationRepository;

    @Before
    public void beforeEachTestRuns() {
        stationRepository = createMock(StationRepository.class);
        mapper = new MapTransportRelationshipsToStages(new RouteCodeToClassMapper(), stationRepository);
    }

    @Test
    @Ignore("Work in progress")
    public void shouldSimpleJourney() {
        List<TransportRelationship> relationships = new LinkedList<>();
        RouteStationNode boardingPoint = RouteStationNode.TestOnly("id2", "routeNameA", "routeIdA", "stationName1");
        RouteStationNode departurePoint = RouteStationNode.TestOnly("id3", "routeNameA", "routeIdA", "stationName2");

        relationships.add(BoardRelationship.TestOnly(2, "someId",
                StationNode.TestOnly("id1", "name1"),
                boardingPoint));
        boolean[] daysRunning = new boolean[1];
        int[] timesRunning = new int[1];
        String id = "id4";
        TramServiceDate startDate = new TramServiceDate(LocalDate.now());
        TramServiceDate endDate = new TramServiceDate(LocalDate.now());
        relationships.add(TramGoesToRelationship.TestOnly("svcId", 18, daysRunning, timesRunning, id, startDate,
                endDate, "dest", boardingPoint, departurePoint));
        replayAll();
        List<TransportStage> result = mapper.mapStages(relationships, 7 * 60);
        verifyAll();
        VehicleStageWithTiming stage = (VehicleStageWithTiming) result.get(0);
        assertNotNull(stage);
    }

}
