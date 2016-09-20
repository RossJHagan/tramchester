package com.tramchester.resources;

import com.codahale.metrics.annotation.Timed;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tramchester.config.TramchesterConfig;
import com.tramchester.domain.ClosedStations;
import com.tramchester.domain.Location;
import com.tramchester.domain.Station;
import com.tramchester.domain.presentation.DisplayStation;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.StationClosureMessage;
import com.tramchester.repository.StationRepository;
import com.tramchester.repository.TransportDataFromFiles;
import com.tramchester.services.SpatialService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import static java.lang.String.format;

@Path("/stations")
@Produces(MediaType.APPLICATION_JSON)
public class StationResource {
    private static final Logger logger = LoggerFactory.getLogger(StationResource.class);
    public static final String TRAMCHESTER_RECENT = "tramchesterRecent";

    private List<Station> allStationsSorted;
    private final SpatialService spatialService;
    private final ClosedStations closedStations;
    private final TramchesterConfig config;
    private final ObjectMapper mapper;
    private final StationRepository stationRepository;

    public StationResource(TransportDataFromFiles transportData, SpatialService spatialService,
                           ClosedStations closedStations, TramchesterConfig config) {
        this.spatialService = spatialService;
        this.closedStations = closedStations;
        this.config = config;
        this.stationRepository = transportData;
        mapper = new ObjectMapper();
        allStationsSorted = new LinkedList<>();
    }

    @GET
    @Timed
    public Response getAll(@CookieParam(TRAMCHESTER_RECENT) Cookie tranchesterRecent) {
        logger.info("Get all stations");

        String recentId = (tranchesterRecent==null) ? "" : tranchesterRecent.getValue();

        List<DisplayStation> displayStations = getStations().stream().
                filter(station -> !station.getId().equals(recentId)).
                map(station -> new DisplayStation(station, SpatialService.ALL_STOPS_PROX_GROUP)).
                collect(Collectors.toList());

        if (!recentId.isEmpty()) {
            Station recentStation = stationRepository.getStation(recentId);
            displayStations.add(new DisplayStation(recentStation, SpatialService.RECENT_GROUP));
        }

        Response response = Response.ok(displayStations).build();
        return response;
    }

    private List<Station> getStations() {
        if (allStationsSorted.isEmpty()) {
            List<Station> rawList = stationRepository.getStations();
            allStationsSorted = rawList.stream().
                    sorted((s1, s2) -> s1.getName().compareTo(s2.getName())).
                    filter(station -> !closedStations.contains(station.getName())).
                    collect(Collectors.toList());
        }
        return allStationsSorted;
    }

    @GET
    @Timed
    @Path("/closures")
    public Response getClosures() {
        logger.info("Get station closures");
        StationClosureMessage stationClosureMessage = new StationClosureMessage(closedStations);
        return Response.ok(stationClosureMessage).build();
    }

    @GET
    @Timed
    @Path("/{id}")
    public Response get(@PathParam("id") String id) {
        logger.info("Get station " + id);
        Location station = stationRepository.getStation(id);
        if (station!=null) {
            return Response.ok(station).build();
        }
        else {
            return Response.status(Response.Status.NOT_FOUND).build();
        }
    }

    @GET
    @Timed
    @Path("/{lat}/{lon}")
    public Response getNearest(@PathParam("lat") double lat, @PathParam("lon") double lon,
                               @CookieParam(TRAMCHESTER_RECENT) Cookie tranchesterRecent) throws JsonProcessingException {
        logger.info(format("Get station at %s,%s ", lat, lon));

        String recentId = (tranchesterRecent==null) ? "" : tranchesterRecent.getValue();

        LatLong latLong = new LatLong(lat,lon);
        List<DisplayStation> orderedStations = spatialService.reorderNearestStations(latLong, getStations());

        if (!recentId.isEmpty()) {
            Station recentStation = stationRepository.getStation(recentId);
            orderedStations.remove(new DisplayStation(recentStation, SpatialService.ALL_STOPS_PROX_GROUP));
            orderedStations.add(0, new DisplayStation(recentStation, SpatialService.RECENT_GROUP));
        }

        if (config.getShowMyLocation()) {
            logger.info("Showing users location in stations list");

            // TODO use MyLocation instead of Station
            Station myLocation = new Station(formId(lat,lon), "", "My Location", latLong, false);
            orderedStations.add(0, new DisplayStation(myLocation, SpatialService.NEARBY));
        }

        return Response.ok(orderedStations).build();
    }

    private String formId(double lat, double lon) throws JsonProcessingException {
        return mapper.writeValueAsString(new LatLong(lat, lon));
    }
}
