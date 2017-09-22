package com.njit.cs656.android_weather;

import cz.msebera.android.httpclient.Header;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

import static android.icu.lang.UCharacter.GraphemeClusterBreak.V;
import static android.provider.UserDictionary.Words.APP_ID;
import static com.njit.cs656.android_weather.R.id.changeCityButton;


public class WeatherController extends AppCompatActivity {

    // App ID to use OpenWeather data
    //final String APP_ID = "e72ca729af228beabd5d20e3b7749713";
    final String API_KEY = "4abae86bf8950a7daddeaabe5243f751";


    // Constants:
    final int REQUEST_CODE = 234;
    final String WEATHER_URL = "http://api.openweathermap.org/data/2.5/weather";

    // Time between location updates (5000 milliseconds or 5 seconds)
    final long MIN_TIME = 5000;
    // Distance between location updates (1000m or 1km)
    final float MIN_DISTANCE = 1000;

    String LOCATION_PROVIDER = LocationManager.GPS_PROVIDER;


    // Member Variables:
    TextView mCityLabel;
    ImageView mWeatherImage;
    TextView mTemperatureLabel;
    ProgressBar mProgressBar;

    LocationManager mLocationManager;
    LocationListener mLocationListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.weather_controller_layout);

        // Linking the elements in the layout to Java code
        mCityLabel = (TextView) findViewById(R.id.locationTV);
        mWeatherImage = (ImageView) findViewById(R.id.weatherSymbolIV);
        mTemperatureLabel = (TextView) findViewById(R.id.tempTV);
        mProgressBar = (ProgressBar) findViewById(R.id.loadingProgressBar);

        ImageButton changeCityButton = (ImageButton) findViewById(R.id.changeCityButton);

        changeCityButton.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(WeatherController.this, ChangeCityController.class);
                startActivity(myIntent);
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d("Weather", "onResume() called");
        Intent myIntent = getIntent();
        String city = myIntent.getStringExtra("City");

        // If city is not empty get the weather fro the city
        if(city != null) {
            getWeatherForNewCity(city);

        } else {
            Log.d("Weather", "Getting weather for current location");
            getWeatherForCurrentLocation();
        }
    }

        private void getWeatherForNewCity(String city) {

        RequestParams params = new RequestParams();
        params.put("q", city);
        params.put("appid", API_KEY);
        getWeatherData(params);
    }

    private void getWeatherForCurrentLocation() {
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        mLocationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                Log.d("Weather", "onLocationChanged() called");
                String longitude = String.valueOf(location.getLongitude());
                String latitude = String.valueOf(location.getLatitude());

                Log.d("Weather", "longitude is: " + longitude);
                Log.d("Weather", "latitude is: " + latitude);

                RequestParams params = new RequestParams();
                params.put("appid", API_KEY);
                params.put("lat", latitude);
                params.put("lon", longitude);
                getWeatherData(params);

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {
                Log.d("Weather", "onProdiverDisabled() called");
            }
        };

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] {Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_CODE);
            return;
        }
        mLocationManager.requestLocationUpdates(LOCATION_PROVIDER, MIN_TIME, MIN_DISTANCE, mLocationListener);

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if(requestCode == REQUEST_CODE) {
            if(grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d("Weather", "onRequstPermissionREsult() - Granted!");
                getWeatherForCurrentLocation();
            } else {
                Log.d("Weather", "onRequstPermissionREsult() - DENIED!");

            }

        }  else {
            Log.d("Weather", "onRequstPermissionREsult() - DENIED!");

        }
    }


    private void getWeatherData(RequestParams params) {

        mProgressBar.setVisibility(View.VISIBLE);

        AsyncHttpClient client = new AsyncHttpClient();
        client.get(WEATHER_URL, params, new JsonHttpResponseHandler() {
            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                Log.d("Weather", "Success! JSON " + response.toString());

                WeatherDataModel weatherData = WeatherDataModel.fromJson(response);

                mProgressBar.setVisibility(View.INVISIBLE);

                updateUI(weatherData);

            }

            @Override
            public void onFailure(int statusCode, Header[] headers, Throwable e, JSONObject response) {
                Log.e("Weather", "Failed " + e.toString());
                Log.d("Weather", "Status Code " + statusCode);
                Toast.makeText(WeatherController.this, "Request Failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(WeatherDataModel weather) {
        mTemperatureLabel.setText(weather.getTemperature());
        mCityLabel.setText(weather.getCity());

        int resourceID = getResources().getIdentifier(weather.getIconName(), "drawable", getPackageName());
        mWeatherImage.setImageResource(resourceID);
    }

    @Override
    protected void onPause() {
        super.onPause();

        if(mLocationListener != null) {
            mLocationManager.removeUpdates(mLocationListener);
        }
    }

}
