package com.example.bastienwcs.webservices;

import android.location.Location;
import android.util.Log;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.json.gson.GsonFactory;
import com.octo.android.robospice.request.googlehttpclient.GoogleHttpClientSpiceRequest;

import java.io.IOException;

/**
 * Created by bastienwcs on 09/10/17.
 */

public class ForecastWeatherRequest extends GoogleHttpClientSpiceRequest<ForecastWeatherModel> {

    private String baseUrl;
    private String lat;
    private String lon;

    public ForecastWeatherRequest(Location loc) {
        super(ForecastWeatherModel.class);
        lat = String.valueOf(loc.getLatitude());
        lon = String.valueOf(loc.getLongitude());
        this.baseUrl = String.format("http://api.openweathermap.org/data/2.5/forecast?lat=%s&lon=%s&appid=%s&output=json",
                lat, lon, "7538d4f273fbca1779fdd5aaab2a50f4");
    }

    @Override
    public ForecastWeatherModel loadDataFromNetwork() throws IOException {
        Log.i("TAG", "Call web service " + baseUrl);
        HttpRequest request = getHttpRequestFactory()//
                .buildGetRequest(new GenericUrl(baseUrl));
        request.setParser(new GsonFactory().createJsonObjectParser());
        return request.execute().parseAs(getResultType());
    }

    /**
     * This method generates a unique cache key for this request. In this case
     * our cache key depends just on the keyword.
     * @return
     */
    public String createCacheKey() {
        return "weather." + lat + "," + lon;
    }
}