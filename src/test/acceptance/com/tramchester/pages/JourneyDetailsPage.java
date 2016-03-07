package com.tramchester.pages;

import org.openqa.selenium.WebDriver;

public class JourneyDetailsPage extends Page {

    public JourneyDetailsPage(WebDriver driver) {
        super(driver);
    }

    public String getSummary() {
        return findElementById("journeyHeader").getText();
    }

    public String getPrompt(int index) {
        return getTextFor("stagePrompt", index);
    }

    public String getInstruction(int index) {
        return getTextFor("stageInstruction", index);
    }

    public String getDuration(int index) {
        return getTextFor("stageDuration", index);
    }

    public String getArrive(int index) {
        return getTextFor("stageArrive", index);
    }

    public String getChange(int index) {
        return getTextFor("change", index);
    }

    public RoutePlannerPage planNewJourney() throws InterruptedException {
        findElementById("newJourney").click();
        return new RoutePlannerPage(driver);
    }

    public RouteDetailsPage backToRouteDetails() {
        findElementById("backToRouteDetails").click();
        return new RouteDetailsPage(driver);
    }
}
