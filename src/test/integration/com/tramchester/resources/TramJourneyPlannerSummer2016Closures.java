package com.tramchester.resources;

import com.tramchester.Dependencies;
import com.tramchester.IntegrationTramTestConfig;
import com.tramchester.Stations;
import com.tramchester.domain.Location;
import com.tramchester.domain.exceptions.TramchesterException;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.junit.*;
import org.junit.rules.Timeout;

import java.io.IOException;

import static org.junit.Assert.assertTrue;

public class TramJourneyPlannerSummer2016Closures extends JourneyPlannerHelper {
    public static final int AM10 = 10 * 60;
    private static Dependencies dependencies;
    private LocalDate date;

    @Rule
    public Timeout globalTimeout = Timeout.seconds(200); // TODO should not have to set this so high

    @BeforeClass
    public static void onceBeforeAnyTestsRun() throws IOException {
        dependencies = new Dependencies();
        dependencies.initialise(new IntegrationTramTestConfig());
    }

    @Before
    public void beforeEachTestRuns() {
        //date = new LocalDate(2016,6,28); // closure starts on the 27th
        date = LocalDate.now();
        planner = dependencies.get(JourneyPlannerResource.class);
    }

    @AfterClass
    public static void OnceAfterAllTestsAreFinished() {
        dependencies.close();
    }

    @Test
    public void shouldFindAltrinchamToDeansgate() throws TramchesterException {
        checkRouteNext7Days(Stations.Altrincham, Stations.Deansgate, date, AM10);
    }

    @Test
    public void shouldFindARochToExSq() throws TramchesterException {
        checkRouteNext7Days(Stations.Rochdale, Stations.ExchangeSquare, date, AM10);
    }

    @Test
    public void shouldFindEndOfLinesToEndOfLinesEast() throws TramchesterException {
        for (Location start : Stations.EndOfTheLineEast) {
            for (Location dest : Stations.EndOfTheLineEast) {
                checkRouteNext7Days(start, dest, date, AM10);
            }
        }
    }

    @Test
    public void shouldFindEndOfLinesToEndOfLinesWest() throws TramchesterException {
        for (Location start : Stations.EndOfTheLineWest) {
            for (Location dest : Stations.EndOfTheLineWest) {
                if (!start.equals(Stations.Eccles) && !dest.equals(Stations.Eccles))
                checkRouteNext7Days(start, dest, date, AM10);
            }
        }
    }

    @Test
    public void shouldFindWalkingRouteInCentre() throws TramchesterException {
        checkRouteNext7Days(Stations.Deansgate, Stations.MarketStreet, date, AM10);
    }

    @Test
    public void shouldCrossCityWithAWalk() throws TramchesterException {
        checkRouteNext7Days(Stations.Altrincham, Stations.Bury, date, AM10);
    }

}
