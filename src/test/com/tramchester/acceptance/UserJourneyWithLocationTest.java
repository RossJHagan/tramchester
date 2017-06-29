package com.tramchester.acceptance;

import com.tramchester.App;
import com.tramchester.acceptance.infra.AcceptanceTestHelper;
import com.tramchester.acceptance.infra.AcceptanceTestRun;
import com.tramchester.acceptance.infra.DriverFactory;
import com.tramchester.acceptance.infra.ProvidesDriver;
import com.tramchester.acceptance.pages.JourneyDetailsPage;
import com.tramchester.acceptance.pages.RouteDetailsPage;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.integration.Stations;
import com.tramchester.integration.resources.JourneyPlannerHelper;
import org.apache.commons.io.FileUtils;
import org.joda.time.LocalDate;
import org.junit.*;
import org.junit.experimental.categories.Category;
import org.junit.rules.TestName;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.assertTrue;

public class UserJourneyWithLocationTest {
    protected static final String configPath = "config/localAcceptance.yml";
    protected int expectedNumberJourneyResults = 3; // depends on frequency and timewindow

    @ClassRule
    public static AcceptanceTestRun testRule = new AcceptanceTestRun(App.class, configPath);

    @Rule
    public TestName testName = new TestName();

    private Path path = Paths.get("geofile.json");
    private String myLocation = "My Location";
    private LatLong nearAltrincham = new LatLong(53.394982299999995D,-2.3581502D);
    private LocalDate when;
    private String url;
    private AcceptanceTestHelper helper;
    private ProvidesDriver providesDriver;

    @Before
    public void beforeEachTestRuns() {
        url = testRule.getUrl();
        providesDriver = DriverFactory.create(true);

        createGeoFile();
        providesDriver.setProfileForGeoFile(path.toAbsolutePath());

        providesDriver.init();
        helper = new AcceptanceTestHelper(providesDriver);

        // TODO offset for when tfgm data is expiring
        when = JourneyPlannerHelper.nextMonday(20);
    }

    @After
    public void afterEachTestRuns() throws IOException {
        Files.deleteIfExists(path);
        providesDriver.commonAfter(testName);
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCheckNearAltrinchamToAshton() throws InterruptedException {

        assertTrue(Files.exists(path));

        String finalStation = "Ashton-Under-Lyne";
        String firstStation = Stations.NavigationRoad.getName();

        List<String> changes = Arrays.asList(firstStation, Stations.PiccadillyGardens.getName());
        List<String> headSignsA = Arrays.asList("","Etihad Campus",finalStation);

        RouteDetailsPage routeDetailsPage = helper.enterRouteSelection(url, myLocation, finalStation, when, "19:47");

        helper.checkDetailsAndJourneysPresent(routeDetailsPage, firstStation, finalStation, changes, false,
                expectedNumberJourneyResults, true, false);

        JourneyDetailsPage journeyDetailsPage = routeDetailsPage.getDetailsFor(0);
        assertTrue(journeyDetailsPage.getSummary().endsWith(" from "+firstStation));
        helper.checkInitialWalkingStage(journeyDetailsPage, firstStation, headSignsA);
        helper.checkStage(journeyDetailsPage, 1, firstStation, finalStation, changes, headSignsA, true);
        helper.checkStage(journeyDetailsPage, 2, firstStation, finalStation, changes, headSignsA, false);

        while(journeyDetailsPage.laterTramEnabled()) {
            journeyDetailsPage.laterTram();
            helper.checkInitialWalkingStage(journeyDetailsPage, firstStation, headSignsA);
            // these vary depending on timing of trams, key thing is to check walking stage is first
//            checkStage(journeyDetailsPage, 1, firstStation, finalStation, changes, headSignsA, true);
//            checkStage(journeyDetailsPage, 2, firstStation, finalStation, changes, headSignsA, false);
        }
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCheckNearAltrinchamToCornbrook() throws InterruptedException {

        assertTrue(Files.exists(path));

        String firstStation = Stations.NavigationRoad.getName();
        List<String> changes = Arrays.asList(firstStation);
        List<String> headSigns = Arrays.asList("","Etihad Campus");

        String finalStation = Stations.Deansgate.getName();

        RouteDetailsPage routeDetailsPage = helper.enterRouteSelection(url, myLocation, finalStation, when, "19:47");

        helper.checkDetailsAndJourneysPresent(routeDetailsPage, firstStation, finalStation, changes, false,
                expectedNumberJourneyResults, true, false);

        JourneyDetailsPage journeyDetailsPage = routeDetailsPage.getDetailsFor(0);
        assertTrue(journeyDetailsPage.getSummary().endsWith(" from "+firstStation));
        helper.checkInitialWalkingStage(journeyDetailsPage, firstStation, headSigns);
        helper.checkStage(journeyDetailsPage, 1, firstStation, finalStation, changes, headSigns, true);
    }

    @Test
    @Category({AcceptanceTest.class})
    public void shouldCopeWithNearbyLocationWhenSelectingMyLocation() throws InterruptedException {
        assertTrue(Files.exists(path));

        List<String> changes = Arrays.asList();

        String finalStation = Stations.NavigationRoad.getName();

        RouteDetailsPage routeDetailsPage = helper.enterRouteSelection(url, myLocation, finalStation, when, "19:47");

        helper.checkDetailsAndJourneysPresent(routeDetailsPage, myLocation, finalStation, changes, false,
                expectedNumberJourneyResults, true, true);

    }

    private void createGeoFile() {
        String json = "{\n" +
                "    \"status\": \"OK\",\n" +
                "    \"accuracy\": 10.0,\n" +
                "    \"location\": {\n" +
                "        \"lat\": " +nearAltrincham.getLat() + ",\n" +
                "        \"lng\": " +nearAltrincham.getLon()+"\n" +
                "     }\n" +
                "}";

        try {
            FileUtils.writeStringToFile(path.toFile(), json);
        } catch (IOException e) {
            // this is asserted later
        }
    }
}