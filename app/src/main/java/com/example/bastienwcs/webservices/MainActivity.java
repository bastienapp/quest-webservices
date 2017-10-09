package com.example.bastienwcs.webservices;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.octo.android.robospice.GsonGoogleHttpClientSpiceService;
import com.octo.android.robospice.SpiceManager;
import com.octo.android.robospice.persistence.DurationInMillis;
import com.octo.android.robospice.persistence.exception.SpiceException;
import com.octo.android.robospice.request.listener.RequestListener;

import java.text.DateFormatSymbols;
import java.util.Calendar;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private static final int PERMISSION_REQUEST_LOCALISATION = 10;

    protected SpiceManager mSpiceManager = new SpiceManager(GsonGoogleHttpClientSpiceService.class);
    private LocationManager mLocationManager = null;
    private LocationListener mLocationListener = null;
    private static final String[] PERMISSIONS = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mLocationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        mLocationListener = new LocationListener() {
            public void onLocationChanged(Location location) {
                downloadWeatherData(new Location(location));
            }

            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            public void onProviderEnabled(String provider) {

            }

            public void onProviderDisabled(String provider) {

            }
        };
    }

    @Override
    protected void onStart() {
        super.onStart();
        mSpiceManager.start(this);
        checkPermission();
    }

    @Override
    protected void onStop() {
        mSpiceManager.shouldStop();
        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_LOCALISATION: {
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Toast.makeText(MainActivity.this,
                            getResources().getString(R.string.permission_granted),
                            Toast.LENGTH_SHORT).show();

                    checkPermission();
                } else {
                    Toast.makeText(MainActivity.this,
                            getResources().getString(R.string.permission_not_granted),
                            Toast.LENGTH_SHORT).show();
                }
            }
        }
    }

    private void checkPermission() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(MainActivity.this,
                    PERMISSIONS, PERMISSION_REQUEST_LOCALISATION);
            return;
        }
        String provider = mLocationManager.getBestProvider(new Criteria(), false);
        Location location = mLocationManager.getLastKnownLocation(provider);
        if (location != null) {
            downloadWeatherData(new Location(location));
        }
        mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, mLocationListener);
    }

    private void downloadWeatherData(Location loc) {
        CurrentWeatherRequest weatherRequest = new CurrentWeatherRequest(loc);
        mSpiceManager.execute(weatherRequest, weatherRequest.createCacheKey(), DurationInMillis.ONE_MINUTE, new CurrentWeatherRequestListener());

        ForecastWeatherRequest forecastRequest = new ForecastWeatherRequest(loc);
        mSpiceManager.execute(forecastRequest, forecastRequest.createCacheKey(), DurationInMillis.ONE_MINUTE, new ForecastWeatherRequestListener());
    }

    private class CurrentWeatherRequestListener implements RequestListener<CurrentWeatherModel> {

        @Override
        public void onRequestFailure(SpiceException e) {

        }

        @Override
        public void onRequestSuccess(CurrentWeatherModel currentWeatherModel) {
            TextView city = findViewById(R.id.city);
            city.setText(currentWeatherModel.getName());

            LinearLayout weatherContainer = findViewById(R.id.weather);
            weatherContainer.removeAllViews();
            Date date = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            for (Weather weather : currentWeatherModel.getWeather()) {
                addWeather(weather, weatherContainer, cal);
            }
        }
    }

    private class ForecastWeatherRequestListener implements RequestListener<ForecastWeatherModel> {

        @Override
        public void onRequestFailure(SpiceException e) {

        }

        @Override
        public void onRequestSuccess(ForecastWeatherModel forecastWeatherModel) {

            LinearLayout weatherContainer = findViewById(R.id.forecast);
            weatherContainer.removeAllViews();
            Date date = new Date();
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            for (List weatherList : forecastWeatherModel.getList().subList(0, 4)) {
                for (Weather weather : weatherList.getWeather()) {
                    cal.add(Calendar.DATE, 1);
                    addWeather(weather, weatherContainer, cal);
                }
            }
        }
    }

    private void addWeather(Weather weather, LinearLayout weatherContainer, Calendar cal) {
        ImageView icon = new ImageView(MainActivity.this);
        icon.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        icon.setScaleType(ImageView.ScaleType.CENTER);

        Glide.with(MainActivity.this)
                .load(String.format("http://openweathermap.org/img/w/%s.png", weather.getIcon()))
                .into(icon);
        weatherContainer.addView(icon);

        TextView currentWeather = new TextView(MainActivity.this);
        int day = cal.get(Calendar.DAY_OF_WEEK);
        currentWeather.setText(String.format("%s : %s",
                DateFormatSymbols.getInstance().getWeekdays()[day], weather.getDescription()));
        weatherContainer.addView(currentWeather);
    }
}
