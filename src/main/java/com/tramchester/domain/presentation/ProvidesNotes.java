package com.tramchester.domain.presentation;

import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.TramServiceDate;
import com.tramchester.domain.TransportMode;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import com.tramchester.domain.presentation.DTO.JourneyDTO;
import com.tramchester.domain.presentation.DTO.PlatformDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
import org.apache.commons.collections4.map.HashedMap;
import org.joda.time.LocalDate;

import java.util.*;

import static java.lang.String.format;

public class ProvidesNotes {
    private final TramchesterConfig config;

    private static final String EMPTY = "<no message>";

    public static final String website = "Please check <a href=\"http://www.metrolink.co.uk/pages/pni.aspx\">TFGM</a> for details.";

    public static String weekend = "At the weekend your journey may be affected by improvement works." + website;

    public static String christmas = "There are changes to Metrolink services during Christmas and New Year." + website;

    public ProvidesNotes(TramchesterConfig config) {
        this.config = config;
    }

    public List<String> createNotesForJourneys(TramServiceDate queryDate, SortedSet<JourneyDTO> decoratedJourneys) {
        List<String> notes = new LinkedList<>();

        AddNonLiveDataNotes(queryDate, notes);

        notes.addAll(addNotesForJourneys(decoratedJourneys));

        return notes;
    }

    public List<String> createNotesForStations(List<StationDTO> stations) {
        List<String> notes = new LinkedList<>();

        TramServiceDate queryDate = new TramServiceDate(LocalDate.now());
        AddNonLiveDataNotes(queryDate, notes);

        notes.addAll(addLiveMessagesFor(stations));

        return notes;
    }

    private Set<String> addLiveMessagesFor(List<StationDTO> stations) {
        // Map: Message -> Location
        Map<String,String> messageMap = new HashedMap<>();
        stations.forEach(stationDTO -> stationDTO.getPlatforms().forEach(platformDTO -> {
            addRelevantMessage(messageMap, platformDTO);
        }));

        return createMessageList(messageMap);
    }

    private void AddNonLiveDataNotes(TramServiceDate queryDate, List<String> notes) {
        if (queryDate.isWeekend()) {
            notes.add(weekend);
        }

        if (queryDate.isChristmasPeriod()) {
            notes.add(christmas);
        }

        notes.addAll(addNotesForStations(config.getClosedStations()));
    }

    private Set<String> addNotesForStations(List<String> closedStations) {
        Set<String> messages = new HashSet<>();
        closedStations.forEach(stationName -> messages.add(format("%s is currently closed. %s", stationName, website)));
        return messages;
    }

    private Set<String> addNotesForJourneys(SortedSet<JourneyDTO> decoratedJourneys) {
        // Map: Message -> Location
        Map<String,String> messageMap = new HashedMap<>();

        decoratedJourneys.stream().forEach(journeyDTO -> journeyDTO.getStages().stream().
                filter(stageDTO -> stageDTO.getMode().equals(TransportMode.Tram)).
                forEach(tramStage -> addRelevantMessage(messageMap, tramStage.getPlatform()))
        );

        return createMessageList(messageMap);
    }

    private Set<String> createMessageList(Map<String, String> messageMap) {
        Set<String> messages = new HashSet<>();
        messageMap.forEach((rawMessage,location) -> {
            if (location.isEmpty()) {
                messages.add(format("'%s' - Metrolink", rawMessage));
            } else {
                messages.add(format("'%s' - %s, Metrolink", rawMessage, location));
            }
        });

        return messages;
    }

    private void addRelevantMessage(Map<String,String> messageMap, PlatformDTO platform) {
        StationDepartureInfo info = platform.getStationDepartureInfo();
        if (info==null) {
            return;
        }

        String rawMessage = info.getMessage();
        if (usefulMessage(rawMessage)) {
            if (messageMap.containsKey(rawMessage)) {
                String existingLocation = messageMap.get(rawMessage);
                if (!existingLocation.equals(info.getLocation())) {
                    messageMap.put(rawMessage, ""); // must be shown at multiple locations, not specific
                }
            } else {
                messageMap.put(rawMessage, info.getLocation()); // initially specific to one location only
            }
        }
    }

    private boolean usefulMessage(String rawMessage) {
        return ! (rawMessage.isEmpty() || EMPTY.equals(rawMessage));
    }

}
