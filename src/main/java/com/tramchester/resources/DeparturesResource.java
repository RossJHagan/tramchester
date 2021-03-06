package com.tramchester.resources;


import com.codahale.metrics.annotation.Timed;
import com.tramchester.domain.presentation.DTO.DepartureDTO;
import com.tramchester.domain.presentation.DTO.DepartureListDTO;
import com.tramchester.domain.presentation.DTO.StationDTO;
import com.tramchester.domain.presentation.LatLong;
import com.tramchester.domain.presentation.ProvidesNotes;
import com.tramchester.mappers.DeparturesMapper;
import com.tramchester.repository.LiveDataRepository;
import com.tramchester.services.SpatialService;
import io.dropwizard.jersey.caching.CacheControl;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.LinkedList;
import java.util.List;
import java.util.SortedSet;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

@Api
@Path("/departures")
@Produces(MediaType.APPLICATION_JSON)
public class DeparturesResource {

    private final SpatialService spatialService;
    private final LiveDataRepository liveDataRepository;
    private final DeparturesMapper departuresMapper;
    private ProvidesNotes providesNotes;
    private final DateTimeZone timeZone;

    public DeparturesResource(SpatialService spatialService, LiveDataRepository liveDataRepository,
                              DeparturesMapper departuresMapper, ProvidesNotes providesNotes) {
        this.spatialService = spatialService;
        this.liveDataRepository = liveDataRepository;
        this.departuresMapper = departuresMapper;
        this.providesNotes = providesNotes;
        timeZone = DateTimeZone.forTimeZone(TimeZone.getTimeZone("Europe/London"));
    }

    @GET
    @Timed
    @Path("/{lat}/{lon}")
    @ApiOperation(value = "Get geographically close departures", response = DepartureDTO.class, responseContainer = "List")
    @CacheControl(maxAge = 30, maxAgeUnit = TimeUnit.SECONDS)
    public Response getNearestDepartures(@PathParam("lat") double lat, @PathParam("lon") double lon) {
        DateTime time = DateTime.now(timeZone);

        LatLong latLong = new LatLong(lat,lon);
        List<StationDTO> nearbyStations = spatialService.getNearestStations(latLong);

        nearbyStations.forEach(station -> liveDataRepository.enrich(station, time));

        SortedSet<DepartureDTO> departuresDTO = departuresMapper.fromStations(nearbyStations);
        List<String> notes = providesNotes.createNotesForStations(nearbyStations);
        DepartureListDTO departureList = new DepartureListDTO(departuresDTO, notes);

        return Response.ok(departureList).build();
    }
}
