package com.tramchester.acceptance;

import com.tramchester.App;
import com.tramchester.acceptance.infra.AcceptanceTestHelper;
import com.tramchester.acceptance.infra.AcceptanceTestRun;
import com.tramchester.acceptance.infra.DriverFactory;
import com.tramchester.acceptance.infra.ProvidesDriver;
import com.tramchester.acceptance.pages.RoutePlannerPage;
import com.tramchester.acceptance.pages.TramsNearMePage;
import com.tramchester.acceptance.pages.WelcomePage;
import com.tramchester.integration.resources.JourneyPlannerHelper;
import org.joda.time.LocalDate;
import org.junit.*;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.io.IOException;

import static com.tramchester.acceptance.UserJourneyTest.getBrowserList;
import static org.junit.Assert.assertTrue;

@RunWith(Parameterized.class)
public class UserJourneyLiveDataTest {
    private static final String configPath = "config/localAcceptance.yml";

    private String url;
    private ProvidesDriver providesDriver;

    @ClassRule
    public static AcceptanceTestRun testRule = new AcceptanceTestRun(App.class, configPath);

    @Rule
    public TestName testName = new TestName();

    @Parameterized.Parameters
    public static Iterable<? extends Object> data() { return getBrowserList();
    }

    @Parameterized.Parameter
    public String browserName;

    @Before
    public void beforeEachTestRuns() throws IOException {
        url = testRule.getUrl();

        providesDriver = DriverFactory.create(true, browserName);

        providesDriver.setStubbedLocation(AcceptanceTestHelper.NearAltrincham);

        providesDriver.init();

    }

    @After
    public void afterEachTestRuns() {
        providesDriver.commonAfter(testName);
    }

    @Test
    public void shouldDisplayDeparturesByStation() {
        WelcomePage welcomePage = providesDriver.getWelcomePage();
        welcomePage.load(url);
        RoutePlannerPage routePlannerPage = welcomePage.begin();

        routePlannerPage.waitForToStops();
        TramsNearMePage nearMePage = routePlannerPage.tramsNearMe();

        assertTrue(nearMePage.showingDeparturesText());
        assertTrue(nearMePage.waitForStations());

        nearMePage.switchView();
        assertTrue(nearMePage.showingStationsText());
        assertTrue(nearMePage.waitForDepartures());

        nearMePage.switchView();
        assertTrue(nearMePage.showingDeparturesText());
        assertTrue(nearMePage.waitForStations());
    }

}
