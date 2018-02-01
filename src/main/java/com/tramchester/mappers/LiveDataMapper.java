package com.tramchester.mappers;

import com.tramchester.domain.liveUpdates.DueTram;
import com.tramchester.domain.liveUpdates.StationDepartureInfo;
import org.joda.time.DateTime;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.LinkedList;
import java.util.List;

import static java.lang.String.format;

public class LiveDataMapper {

    private int MAX_DUE_TRAMS = 4;

    public List<StationDepartureInfo> map(String rawJson) throws ParseException {
        List<StationDepartureInfo> result = new LinkedList<>();

        JSONParser jsonParser = new JSONParser();
        JSONObject parsed = (JSONObject)jsonParser.parse(rawJson);
        JSONArray infoList = (JSONArray) parsed.get("value");

        if (infoList!=null) {
            for (int i = 0; i < infoList.size(); i++) {
                result.add(parseItem((JSONObject) infoList.get(i)));
            }
        }

        return result;
    }

    private StationDepartureInfo parseItem(JSONObject jsonObject) {
        Long displayId = (Long) jsonObject.get("Id");
        String lineName = (String) jsonObject.get("Line");
        String stationPlatform = (String) jsonObject.get("AtcoCode");
        String message = (String) jsonObject.get("MessageBoard");
        String dateString = (String) jsonObject.get("LastUpdated");
        String location = (String)jsonObject.get("StationLocation");
        DateTime lastUpdate = DateTime.parse(dateString);
        StationDepartureInfo departureInfo = new StationDepartureInfo(displayId.toString(), lineName, stationPlatform,
                location, message, lastUpdate);
        parseDueTrams(jsonObject,departureInfo);
        return departureInfo;
    }

    private void parseDueTrams(JSONObject jsonObject, StationDepartureInfo departureInfo) {
        for (int i = 0; i < MAX_DUE_TRAMS; i++) {
            String dest = getField(jsonObject, i, "Dest");
            if (dest.length()>0) {
                String status = getField(jsonObject, i, "Status");
                String waitString = getField(jsonObject, i, "Wait");
                int wait = Integer.parseInt(waitString);
                String carriages = getField(jsonObject, i, "Carriages");
                DueTram dueTram = new DueTram(dest, status, wait, carriages, departureInfo.getLastUpdate());
                departureInfo.addDueTram(dueTram);
            }
        }
    }

    private String getField(JSONObject jsonObject, int i, String name) {
        String destKey = format("%s%d", name, i);
        return (String) jsonObject.get(destKey);
    }
}
