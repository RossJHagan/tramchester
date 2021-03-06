package com.tramchester.integration.mappers;

import com.tramchester.Dependencies;
import com.tramchester.domain.*;
import com.tramchester.domain.exceptions.TramchesterException;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.JourneyPlanRepresentation;
import com.tramchester.domain.presentation.DTO.factory.JourneyDTOFactory;
import com.tramchester.domain.presentation.DTO.factory.StageDTOFactory;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.graph.RouteCalculator;
import com.tramchester.integration.BusTest;
import com.tramchester.integration.IntegrationBusTestConfig;
import com.tramchester.livedata.LiveDataEnricher;
import com.tramchester.mappers.HeadsignMapper;
import com.tramchester.mappers.JourneysMapper;
import com.tramchester.mappers.TramJourneyResponseMapper;
import com.tramchester.repository.LiveDataRepository;
import org.joda.time.LocalDate;
import org.junit.*;
import org.junit.experimental.categories.Category;

import java.io.IOException;
import java.util.*;

import static junit.framework.TestCase.assertEquals;
import static org.joda.time.DateTimeConstants.MONDAY;

public class JourneyResponseMapperForBusTest extends JourneyResponseMapperTest {

    private static Dependencies dependencies;
    private JourneysMapper mapper;
    private Set<RawJourney> journeys;
    private List<RawStage> stages;

    private Location stockportBusStation = new Station("1800STBS001", "stockportArea", "Bus station", new LatLong(1.5, 1.5), false);
    private Location stockportBridgefieldStreet = new Station("1800SG18471", "stockportArea", "Bridgefield Street",
            new LatLong(1.5, 1.5), false);
    private List<String> notes;
    private LiveDataRepository liveDataRepository;

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationBusTestConfig());
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Before
    public void beforeEachTestRuns() {
        notes = new LinkedList<>();
        mapper = dependencies.get(JourneysMapper.class);
        routeCalculator = dependencies.get(RouteCalculator.class);
        liveDataRepository = dependencies.get(LiveDataRepository.class);
        journeys = new HashSet<>();
        stages = new LinkedList<>();
    }

    @Test
    @Category({BusTest.class})
    @Ignore("Work in progress")
    public void shouldMapStockportCircularJourney() throws TramchesterException {
        LocalDate now = LocalDate.now();
        int offset = now.getDayOfWeek()-MONDAY;
        LocalDate when = now.plusDays(offset);
        String svcId = findServiceId(stockportBusStation.getId(), stockportBridgefieldStreet.getId(), when, 571);
        //String svcId = "Serv002953"; // use above when timetable changes to find new svc id

        JourneyPlanRepresentation result = getJourneyPlanRepresentation(stockportBusStation, stockportBridgefieldStreet,
                svcId, 42, 571, new TramServiceDate(when));

        assertEquals(1,result.getJourneys().size());
    }

    private JourneyPlanRepresentation getJourneyPlanRepresentation(Location begin, Location end, String svcId,
                                                                   int cost, int minutesFromMidnight, TramServiceDate queryDate) throws TramchesterException {

        RawVehicleStage busStage = new RawVehicleStage(begin, "route text", TransportMode.Bus, "cssClass");
        busStage.setServiceId(svcId);
        busStage.setLastStation(end);
        busStage.setCost(cost);

        stages.add(busStage);
        journeys.add(new RawJourney(stages, minutesFromMidnight));

        LiveDataEnricher liveDataEnricher = new LiveDataEnricher(liveDataRepository, queryDate, minutesFromMidnight);
        StageDTOFactory stageFactory = new StageDTOFactory(liveDataEnricher);
        HeadsignMapper headsignMapper = new HeadsignMapper();
        JourneyDTOFactory factory = new JourneyDTOFactory(stageFactory, headsignMapper);
        SortedSet<JourneyDTO> mapped = mapper.map(factory, journeys, 30);
        return new JourneyPlanRepresentation(mapped, notes);
    }
}
