package com.tramchester.acceptance;

import com.tramchester.App;
import com.tramchester.acceptance.infra.*;
import com.tramchester.acceptance.pages.JourneyDetailsPage;
import com.tramchester.acceptance.pages.RouteDetailsPage;
import com.tramchester.acceptance.pages.RoutePlannerPage;
import com.tramchester.acceptance.pages.WelcomePage;
import com.tramchester.integration.Stations;
import com.tramchester.integration.resources.FeedInfoResourceTest;
import com.tramchester.integration.resources.JourneyPlannerHelper;
import org.assertj.core.util.Lists;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.*;
import org.junit.rules.TestName;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.WebElement;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringEndsWith.endsWith;
import static org.junit.Assert.*;

@RunWith(Parameterized.class)
public class UserJourneyTest {
    private static final String configPath = "config/localAcceptance.yml";
    private int expectedNumberJourneyResults = 3; // depends on frequency and timewindow

    @ClassRule
    public static AcceptanceTestRun testRule = new AcceptanceTestRun(App.class, configPath);

    private final String bury = Stations.Bury.getName();

    @Rule
    public TestName testName = new TestName();

    private final String altrincham = Stations.Altrincham.getName();
    private final String deansgate = Stations.Deansgate.getName();
    private final String cornbrook = Stations.Cornbrook.getName();

    private LocalDate nextTuesday;
    private String url;
    private AcceptanceTestHelper helper;
    private ProvidesDriver providesDriver;

    // TOOD add firefox back in here
    @Parameters
    public static Iterable<? extends Object> data() {
        return Arrays.asList("chrome");
    }

    @Parameterized.Parameter
    public String browserName;

    @Before
    public void beforeEachTestRuns() {
        url = testRule.getUrl();

        providesDriver = DriverFactory.create(false, browserName);
        providesDriver.init();
        helper = new AcceptanceTestHelper(providesDriver);

        // TODO offset for when tfgm data is expiring
        nextTuesday = JourneyPlannerHelper.nextTuesday(0);
    }

    @After
    public void afterEachTestRuns() {
        providesDriver.commonAfter(testName);
    }

    @Test
    public void shouldRedirectDirectToJourneyPageAfterFirstVisit() throws UnsupportedEncodingException {
        WelcomePage welcomePage = providesDriver.getWelcomePage();
        welcomePage.load(testRule.getUrl());
        assertTrue(welcomePage.hasBeginLink());

        assertTrue(providesDriver.getCookieNamed("tramchesterVisited")==null);
        welcomePage.begin();

        // cookie should now be set
        Cookie cookie = providesDriver.getCookieNamed("tramchesterVisited");
        String cookieContents = URLDecoder.decode(cookie.getValue(), "utf8");
        assertEquals("{\"visited\":true}", cookieContents);

        // check redirect
        RoutePlannerPage redirectedPage = providesDriver.getRoutePlannerPage();
        redirectedPage.load(testRule.getUrl());
        redirectedPage.waitForToStops();
    }

    @Test
    public void shouldCheckAltrinchamToBuryThenBackToStart() throws InterruptedException {
        List<String> headSigns = Arrays.asList("Bury");
        TramJourney tramJourney = new TramJourney(altrincham, bury, nextTuesday, LocalTime.parse("10:15"));
        TramJourneyExpectations expectations = new TramJourneyExpectations(headSigns, expectedNumberJourneyResults);

        JourneyDetailsPage journeyDetailsPage = helper.checkJourney(url, tramJourney,
                expectations, 0, false);
        RoutePlannerPage plannerPage = journeyDetailsPage.planNewJourney();
        plannerPage.waitForToStops();
        // check values remembered
        assertEquals(altrincham,plannerPage.getFromStop());
        assertEquals(bury,plannerPage.getToStop());
        assertEquals("10:15",plannerPage.getTime());

        // check recents are set
        List<WebElement> recentFrom = plannerPage.getRecentFromStops();
        assertEquals(2, recentFrom.size());
        //
        // Need to select an element other than alty for 'from' recent stops to show in 'to'
        plannerPage.setFromStop(Stations.Ashton.getName());
        List<WebElement> recentTo = plannerPage.getRecentToStops();
        assertEquals(2, recentTo.size());
    }

    @Test
    public void shouldHideStationInToListWhenSelectedInFromList() {
        WelcomePage welcomePage = providesDriver.getWelcomePage();
        welcomePage.load(testRule.getUrl());

        RoutePlannerPage routePlannerPage = welcomePage.begin();

        routePlannerPage.waitForToStops();
        routePlannerPage.setFromStop(altrincham);
        List<String> toStops = routePlannerPage.getToStops();
        assertFalse(toStops.contains(altrincham));
        assertTrue(toStops.contains(cornbrook));

        routePlannerPage.setFromStop(cornbrook);
        toStops = routePlannerPage.getToStops();
        assertTrue(toStops.contains(altrincham));
        assertFalse(toStops.contains(cornbrook));
    }

    @Test
    public void shouldShowNoRoutesMessage() throws InterruptedException {
        WelcomePage welcomePage = providesDriver.getWelcomePage();
        welcomePage.load(testRule.getUrl());

        LocalTime time = LocalTime.parse("03:00");

        RoutePlannerPage routePlannerPage = welcomePage.begin();

        routePlannerPage.waitForToStops();
        routePlannerPage.setFromStop(altrincham);
        routePlannerPage.setToStop(cornbrook);
        routePlannerPage.setTime(time);
        routePlannerPage.setDate(nextTuesday);

        RouteDetailsPage detailsPage = routePlannerPage.submit();
        assertTrue(detailsPage.waitForError());

    }

    @Test
    public void shouldSetDateAndTimeCorrectly() throws InterruptedException {
        WelcomePage welcomePage = providesDriver.getWelcomePage();
        welcomePage.load(testRule.getUrl());

        RoutePlannerPage routePlannerPage = welcomePage.begin();

        routePlannerPage.waitForToStops();
        routePlannerPage.setFromStop(altrincham);
        routePlannerPage.setToStop(cornbrook);

        LocalTime timeA = LocalTime.parse("03:15");
        LocalTime timeB = LocalTime.parse("21:45");

        routePlannerPage.setTime(timeA);
        LocalTime setTime = LocalTime.parse(routePlannerPage.getTime());
        assertEquals(timeA, setTime);

        routePlannerPage.setTime(timeB);
        setTime = LocalTime.parse(routePlannerPage.getTime());
        assertEquals(timeB, setTime);

        // date should initially be set to today
        LocalDate setDate = LocalDate.parse(routePlannerPage.getDate());
        assertEquals(LocalDate.now(),setDate);

        routePlannerPage.setDate(nextTuesday);
        setDate = LocalDate.parse(routePlannerPage.getDate());
        assertEquals(nextTuesday,setDate);

    }

    @Test
    public void shouldCheckAirportToDeangateThenBackToRoute() throws InterruptedException {
        List<String> headSigns = Arrays.asList("Victoria");
        TramJourney tramJourney = new TramJourney(Stations.ManAirport.getName(), deansgate, nextTuesday,
                LocalTime.parse("10:15"));
        TramJourneyExpectations tramJourneyExpectations = new TramJourneyExpectations(headSigns, expectedNumberJourneyResults);

        JourneyDetailsPage journeyDetailsPage = helper.checkJourney(url, tramJourney,
                tramJourneyExpectations,
                0, false);
        RouteDetailsPage routeDetailsPage = journeyDetailsPage.backToRouteDetails();
        routeDetailsPage.waitForRoutes();
    }

    @Test
    public void shouldHaveSecondCityCrossingRoutes() throws InterruptedException {
        List<String> headsignRochdale = Arrays.asList("Shaw and Crompton");

        TramJourney tramJourney = new TramJourney(Stations.StPetersSquare.getName(), Stations.ExchangeSquare.getName(),
                nextTuesday, LocalTime.parse("10:15"));
        TramJourneyExpectations tramJourneyExpectations = new TramJourneyExpectations(headsignRochdale, expectedNumberJourneyResults);

        JourneyDetailsPage detailsPage = helper.checkJourney(url, tramJourney, tramJourneyExpectations, 0, false);
        String instruction = detailsPage.getInstruction(0);
        assertTrue(instruction.contains("Tram from Platform"));

    }

    @Test
    public void shouldCheckAltrinchamToDeansgate() throws InterruptedException {

        List<String> headsignEtihadCampus = Arrays.asList("Piccadilly");
        List<String> headSignsBury = Arrays.asList("Bury");
        TramJourney tramJourney = new TramJourney(altrincham, deansgate, nextTuesday, LocalTime.parse("10:15"));

        RouteDetailsPage routeDetailsPage = helper.checkJourney(url, tramJourney,
                new TramJourneyExpectations(headSignsBury, expectedNumberJourneyResults), 0, false)
                .backToRouteDetails();

        List<String> noChanges = new LinkedList<>();
        routeDetailsPage = helper.checkJourneyDetailsPage(routeDetailsPage, altrincham, deansgate, noChanges,
                headsignEtihadCampus, 1).backToRouteDetails();

        helper.checkJourneyDetailsPage(routeDetailsPage, altrincham, deansgate, noChanges, headSignsBury, 2);
    }

    @Test
    public void shouldDisplayNotNotesOnWeekday() throws InterruptedException {

        RouteDetailsPage routeDetailsPage = helper.enterRouteSelection(url, new TramJourney(altrincham, deansgate, nextTuesday,
                LocalTime.parse("10:00")));
        assertTrue(routeDetailsPage.waitForRoutes());
        assertFalse(routeDetailsPage.notesPresent());
    }

    @Test
    public void shouldDisplayNotesOnSaturday() throws InterruptedException {
        LocalDate aSaturday = nextTuesday.minusDays(3);

        RouteDetailsPage routeDetailsPage = helper.enterRouteSelection(url, new TramJourney(altrincham, deansgate, aSaturday,
                LocalTime.parse("10:00")));
        checkForWeekendNotes(routeDetailsPage);
    }

    @Test
    public void shouldDisplayNotesOnSunday() throws InterruptedException {
        LocalDate aSunday = nextTuesday.minusDays(2);

        RouteDetailsPage routeDetailsPage = helper.enterRouteSelection(url, new TramJourney(altrincham, deansgate, aSunday,
                LocalTime.parse("10:00")));
        checkForWeekendNotes(routeDetailsPage);
    }

    @Test
    public void shouldCheckAshtonToPiccadilyGardens() throws InterruptedException {
        List<String> changes = new LinkedList<>();
        List<String> headSigns = Arrays.asList("Eccles");

        String ashton = Stations.Ashton.getName();
        String piccadilly = Stations.PiccadillyGardens.getName();
        TramJourney tramJourney = new TramJourney(ashton, piccadilly, nextTuesday, LocalTime.parse("10:15"));

        JourneyDetailsPage journeyDetailsPage = helper.checkJourney(url, tramJourney,
                new TramJourneyExpectations(changes,headSigns, expectedNumberJourneyResults), 0, false);

        assertTrue(journeyDetailsPage.laterTramEnabled());
        assertFalse(journeyDetailsPage.earlierTramEnabled());

        assertThat(journeyDetailsPage.getSummary(), endsWith(ashton));
        for (int index = 0; index < headSigns.size(); index++) {
            helper.checkStage(journeyDetailsPage, index, ashton, piccadilly,
                    changes, headSigns, false);
        }

        journeyDetailsPage.laterTram();
        assertTrue(journeyDetailsPage.laterTramEnabled());
        assertTrue(journeyDetailsPage.earlierTramEnabled());

        journeyDetailsPage.earlierTram();
        assertTrue(journeyDetailsPage.laterTramEnabled());
        assertFalse(journeyDetailsPage.earlierTramEnabled());

        // cycle through all later journeys
        int count = 0;
        while(journeyDetailsPage.laterTramEnabled()) {
            LocalTime firstTime = journeyDetailsPage.getTime();
            journeyDetailsPage.laterTram();
            assertTrue(firstTime.isBefore(journeyDetailsPage.getTime()));
            count++;
        }

        // back through all earlier joruneys
        while(journeyDetailsPage.earlierTramEnabled()) {
            LocalTime beforeTime = journeyDetailsPage.getTime();
            journeyDetailsPage.earlierTram();
            assertTrue(beforeTime.isAfter(journeyDetailsPage.getTime()));
            count--;
        }

        assertEquals(0,count);
    }

    @Test
    public void shouldCheckAltrinchamToExchangeSquare() throws InterruptedException {
        List<String> changes = Arrays.asList(Stations.Deansgate.getName());
        List<String> headSigns = Arrays.asList("Bury");

        TramJourney tramJourney = new TramJourney(altrincham, Stations.ExchangeSquare.getName(), nextTuesday,
                LocalTime.parse("10:15"));
        TramJourneyExpectations expectations = new TramJourneyExpectations(changes, headSigns, expectedNumberJourneyResults);

        helper.checkJourney(url, tramJourney, expectations, 0, false);
    }

    @Test
    public void shouldHaveBuildAndVersionNumberInFooter() {
        String build = System.getenv("TRAVIS_BUILD_NUMBER");
        if (build==null) {
            build = "0";
        }

        RoutePlannerPage page = providesDriver.getWelcomePage().load(testRule.getUrl()).begin();

        String result = page.findElementById("build").getText();
        assertEquals("Build 1."+build, result);

        String dataBegin = page.getValidFrom();
        assertEquals(" From: "+ FeedInfoResourceTest.validFrom.toString("YYYY-MM-dd"), dataBegin);

        String dataEnd = page.getValidUntil();
        assertEquals(" Until: " + FeedInfoResourceTest.validUntil.toString("YYYY-MM-dd"), dataEnd);

    }

    private void checkForWeekendNotes(RouteDetailsPage routeDetailsPage) {
        List<String> notes = getNotes(routeDetailsPage);
        assertThat(notes,hasItem("At the weekend your journey may be affected by improvement works.Please check TFGM for details."));
    }

    private List<String> getNotes(RouteDetailsPage routeDetailsPage) {
        return routeDetailsPage.getAllNotes();
    }

}
