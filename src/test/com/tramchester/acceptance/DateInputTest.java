package com.tramchester.acceptance;

import com.tramchester.acceptance.infra.ProvidesChromeDateInput;
import com.tramchester.acceptance.infra.ProvidesFirefoxDateInput;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;
import org.junit.Test;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DateInputTest {

    ProvidesFirefoxDateInput firefoxProvider = new ProvidesFirefoxDateInput();
    ProvidesChromeDateInput chromeProvider = new ProvidesChromeDateInput();

    @Test
    public void shouldGetFirefoxDateCorrectly() {
        LocalDate date = new LocalDate(2019, 11, 30);
        String result = firefoxProvider.createDateInput(date);

        // UK is 2019-11-30
        assertEquals(10, result.length());
        assertTrue(result.contains("30"));
        assertTrue(result.contains("11"));
        assertTrue(result.contains("2019"));
    }

    @Test
    public void shouldFirefoxTimeCorrecly() {
        String result = firefoxProvider.createTimeFormat(new LocalTime(14,45));
        assertTrue(result, result.startsWith("14"));
        assertTrue(result, result.endsWith("45"));
    }

    @Test
    public void shouldGetChromeDateCorrectly() {
        LocalDate date = new LocalDate(2019, 11, 30);
        String result = chromeProvider.createDateInput(date);

        // actual ordering is locale specific, which is needed to support browser running in other locals i.e. on CI box
        assertEquals(6, result.length());
        assertTrue(result.contains("30"));
        assertTrue(result.contains("11"));
        assertFalse(result.contains("2019"));
    }

    @Test
    public void shouldGetChromeTimeCorrectly() {
        String result = firefoxProvider.createTimeFormat(new LocalTime(16,55));
        assertTrue(result, result.startsWith("16"));
        assertTrue(result, result.endsWith("55"));
    }


}
