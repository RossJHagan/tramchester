package com.tramchester.domain;

import com.tramchester.dataimport.data.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

public class TransportData {
    private static final Logger logger = LoggerFactory.getLogger(TransportData.class);
    private HashMap<String, Trip> trips = new HashMap<>();
    private HashMap<String, Station> stations = new HashMap<>();
    private HashMap<String, Service> services = new HashMap<>();
    private HashMap<String, Route> routes = new HashMap<>();


    public TransportData(Stream<StopData> stopDataList, Stream<RouteData> routeDataList, Stream<TripData> tripDataList,
                         Stream<StopTimeData> stopTimeDataList, Stream<CalendarData> calendarDataList) {
        stopDataList.forEach((stopData) -> {
            if (!stations.keySet().contains(stopData.getId())) {
                stations.put(stopData.getId(), new Station(stopData.getId(), stopData.getCode(), stopData.getName(), stopData.getLatitude(), stopData.getLongitude()));
            }
        } );

        routeDataList.forEach((routeData) -> {
            Route route = new Route(routeData.getId(), routeData.getCode(), routeData.getName());
            routes.put(route.getId(), route);
        } );

        tripDataList.forEach((tripData) -> {
            Trip trip = getTrip(tripData.getTripId(), tripData.getTripHeadsign());
            Service service = getService(tripData.getServiceId());
            Route route = routes.get(tripData.getRouteId());
            if (route != null) {
                service.addTrip(trip);
                route.addService(service);
            }
        });

        stopTimeDataList.forEach((stopTimeData) -> {
            Trip trip = getTrip(stopTimeData.getTripId());

            Stop stop = new Stop(stopTimeData.getArrivalTime(),
                    stopTimeData.getDepartureTime(),
                    stations.get(stopTimeData.getStopId()),
                    stopTimeData.getStopSequence(),
                    getStopType(stopTimeData),
                    stopTimeData.getMinutesFromMidnight());

            trip.addStop(stop);
        });

        calendarDataList.forEach((calendar) -> {
            Service service = services.get(calendar.getServiceId());

            if (service != null) {
              //  if (calendar.getStart().equals(new DateTime(2015, 01, 05, 0, 0, 0))) {
                    service.setDays(
                            calendar.isMonday(),
                            calendar.isTuesday(),
                            calendar.isWednesday(),
                            calendar.isThursday(),
                            calendar.isFriday(),
                            calendar.isSaturday(),
                            calendar.isSunday()
                    );
//                } else {
//                    services.remove(calendar.getServiceId());
//                }
            }
        } );

    }

    private Trip getTrip(String tripId, String tripHeadsign) {
        if (!trips.keySet().contains(tripId)) {
            trips.put(tripId, new Trip(tripId, tripHeadsign));
        }
        return trips.get(tripId);
    }


    private Service getService(String serviceId) {
        if (!services.keySet().contains(serviceId)) {
            services.put(serviceId, new Service(serviceId));
        }
        return services.get(serviceId);
    }

    public HashMap<String, Route> getRoutes() {
        return routes;
    }

    private StopType getStopType(StopTimeData stopTimeData) {
        if (stopTimeData.getPickupType().equals("0") && stopTimeData.getDropOffType().equals("1")) {
            return StopType.START;
        } else if (stopTimeData.getPickupType() == "1" && stopTimeData.getDropOffType() == "0") {
            return StopType.END;
        }
        return StopType.MIDDLE;

    }

    private Trip getTrip(String tripId) {

        return trips.get(tripId);
    }

    public List<Station> getStations() {
        ArrayList<Station> stationList = new ArrayList<>();
        stationList.addAll(stations.values());
        return stationList;
    }

    public Station getStation(String stationId) {
        return stations.get(stationId);
    }

    public List<ServiceTime> getTimes(String serviceId, String firstStationId, String lastStationId, int minutesFromMidnight) {
        logger.info(String.format("Get times for service %s from %s to %s at minutes past %s",
                serviceId, firstStationId, lastStationId, minutesFromMidnight));
        List<ServiceTime> serviceTimes = new ArrayList<>();
        Service service = services.get(serviceId);

        List<Trip> tripsAfter = service.getTripsAfter(firstStationId, lastStationId, minutesFromMidnight);
        for (Trip trip : tripsAfter) {
            Stop firstStop = trip.getStop(firstStationId);
            Stop lastStop = trip.getStop(lastStationId);
            int fromMidnight = firstStop.getMinutesFromMidnight();

            ServiceTime serviceTime = new ServiceTime(firstStop.getDepartureTime(),
                    lastStop.getArrivalTime(), serviceId, trip.getHeadSign(), fromMidnight);

            logger.info(String.format("Add service time: %s ", serviceTime));
            serviceTimes.add(serviceTime);
        }
        return serviceTimes;
    }
}
