package com.tramchester.unit.domain;

import com.tramchester.domain.DaysOfWeek;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.exceptions.TramchesterException;
import org.joda.time.LocalDate;
import org.junit.Test;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

public class TramServiceDateTest {

    @Test
    public void shouldInitiateWithDate() throws Exception {
        LocalDate date = new LocalDate(2014, 10, 25);
        TramServiceDate tramServiceDate = new TramServiceDate(date);

        assertThat(tramServiceDate.getDate()).isEqualTo(date);
        assertThat(tramServiceDate.getStringDate()).isEqualTo("20141025");
    }

    @Test
    public void shouldInitiateWithStringDate() throws Exception {
        LocalDate date = new LocalDate(2014, 10, 25);
        TramServiceDate tramServiceDate = new TramServiceDate("20141025");

        assertThat(tramServiceDate.getDate()).isEqualTo(date);
        assertThat(tramServiceDate.getStringDate()).isEqualTo("20141025");
    }

    @Test
    public void shouldGetDayOfWeek() throws TramchesterException {
        TramServiceDate tramServiceDate = new TramServiceDate(new LocalDate(2016, 6, 13));
        assertThat(tramServiceDate.getDay()).isEqualTo(DaysOfWeek.Monday);

        tramServiceDate = new TramServiceDate(new LocalDate(2017, 1, 1));
        assertThat(tramServiceDate.getDay()).isEqualTo(DaysOfWeek.Sunday);
    }

    @Test
    public void shouldCheckBetween() {
        LocalDate aDate = new LocalDate(2016, 6, 13);
        TramServiceDate tramServiceDate = new TramServiceDate(aDate);

        assertTrue(tramServiceDate.within(aDate, aDate.plusDays(1)));
        assertTrue(tramServiceDate.within(aDate.minusDays(1), aDate));
        assertTrue(tramServiceDate.within(aDate, aDate));
        assertTrue(tramServiceDate.within(aDate.minusDays(1), aDate.plusDays(1)));
        assertFalse(tramServiceDate.within(aDate.minusDays(2), aDate.minusDays(1)));
        assertFalse(tramServiceDate.within(aDate.plusDays(1), aDate.plusDays(2)));

    }
}