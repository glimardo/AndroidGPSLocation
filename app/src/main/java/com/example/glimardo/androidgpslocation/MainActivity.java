package com.example.glimardo.androidgpslocation;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Geocoder;
import android.location.Location;
import android.location.Address;
import android.location.LocationManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import java.io.IOException;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    private static final String TAG = MainActivity.class.getSimpleName();
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;
    private Location mLastLocation;

    private GoogleApiClient mGoogleApiClient;
    private boolean mRequestingLocationUpdates = false;
    private LocationRequest mLocationRequest;

    private static int UPDATE_INTERVAL = 10000; // 10 sec
    private static int FATEST_INTERVAL = 5000; // 5 sec
    private static int DISPLACEMENT = 10; // 10 meters

    private TextView lblLocation;
    private Button btnShowLocation, btnStartLocationUpdates;

    private Geocoder geocoder = null;
    private TextView lblPlace;
    private TextView lblDateTime;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        Log.i("Sono nel metodo ", "onCreate");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        lblLocation = (TextView) findViewById(R.id.lblLocation);
        btnShowLocation = (Button) findViewById(R.id.btnShowLocation);
        btnStartLocationUpdates = (Button) findViewById(R.id.btnLocationUpdates);

        lblPlace = (TextView) findViewById(R.id.lblPlace);
        lblDateTime = (TextView) findViewById(R.id.lblDateTime);

        if (checkPlayServices()) {

            Log.i("avvio il metodo ", "buildGoogleApiClient");
            buildGoogleApiClient();

            createLocationRequest();
            Log.i("avvio il metodo ", "createLocationRequest");
        }

        btnShowLocation.setOnClickListener(new View.OnClickListener() {


            @Override
            public void onClick(View v) {
                Log.i("click sul bottone ", "btnShowLocation");
                Log.i("avvio il metodo ", "displayLocation");
                try {

                    getGps();
                    getNetwork();

                    displayLocation();
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

        btnStartLocationUpdates.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                Log.i("click sul bottone ", "btnStartLocationUpdates");
                Log.i("avvio il metodo ", "togglePeriodicLocationUpdates");
                togglePeriodicLocationUpdates();
            }
        });
    }

    private void displayLocation() throws IOException {

        Log.i("sono nel metodo ", "displayLocation");
        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient);
        Log.i("valore mLastLocation: ", String.valueOf(mLastLocation));

        if (mLastLocation != null) {
            double latitude = mLastLocation.getLatitude();
            double longitude = mLastLocation.getLongitude();

            lblLocation.setText("Latitudine: " + latitude + "\n" + " Longitudine: " + longitude);
            Log.i("latitude: ", String.valueOf(latitude));
            Log.i("longitude: ", String.valueOf(longitude));

            lblPlace.setText(getAddress(latitude,longitude));
            lblDateTime.setText(getDate());


        } else {

            lblLocation
                    .setText("(Couldn't get the location. Make sure location is enabled on the device)");
            Log.i("ERRORE: ", "NON SI RIESCE AD OTTENERE LA GEOLOCALIZZAZIONE");
        }
    }

    protected synchronized void buildGoogleApiClient() {
        Log.i("sono nel metodo: ", "buildGoogleApiClient");

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API).build();

        Log.i("mGoogleApiClient: ", String.valueOf(mGoogleApiClient));
    }

    private boolean checkPlayServices() {

        Log.i("sono nel metodo: ", "checkPlayServices");

        GoogleApiAvailability googleAPI = GoogleApiAvailability.getInstance();
        int resultCode = googleAPI.isGooglePlayServicesAvailable(this);

        Log.i("valore resultCode: ", String.valueOf(resultCode));

        if (resultCode != ConnectionResult.SUCCESS) {
            if (googleAPI.isUserResolvableError(resultCode)) {
                googleAPI.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST).show();
                Log.i("sono nel ramo OK ", "dell'if");
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                Log.i("sono nel ramo KO ", "dell'if");
                finish();
            }
            return false;
        }
        return true;
    }

    private void togglePeriodicLocationUpdates() {

        Log.i("sono nel metodo: ", "togglePeriodicLocationUpdates");

        Log.i("mRequestingLocationUp: ", String.valueOf(mRequestingLocationUpdates));

        if (!mRequestingLocationUpdates) {
            // Changing the button text
            btnStartLocationUpdates
                    .setText(getString(R.string.btn_stop_location_updates));

            mRequestingLocationUpdates = true;

            Log.i("lancio il metodo: ", "startLocationUpdates");
            // Starting the location updates
            startLocationUpdates();

            Log.d(TAG, "Periodic location updates started!");

        } else {
            // Changing the button text
            btnStartLocationUpdates
                    .setText(getString(R.string.btn_start_location_updates));

            mRequestingLocationUpdates = false;
            Log.i("lancio il metodo: ", "stopLocationUpdates");
            // Stopping the location updates
            stopLocationUpdates();

            Log.d(TAG, "Periodic location updates stopped!");
        }
    }

    protected void createLocationRequest() {

        Log.i("sono nel metodo: ", "createLocationRequest");

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FATEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    protected void startLocationUpdates() {

        Log.i("sono nel metodo: ", "startLocationUpdates");

        if (ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        Log.i("lancio il metodo: ", "LocationServices.FusedLocationApi.requestLocationUpdates");
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);



    }

    protected void stopLocationUpdates() {

        Log.i("sono nel metodo: ", "stopLocationUpdates");

        Log.i("lancio il metodo: ", "LocationServices.FusedLocationApi.removeLocationUpdates");

        LocationServices.FusedLocationApi.removeLocationUpdates(
                mGoogleApiClient, this);
    }

    protected String getAddress(double latitude, double longitude) throws IOException {

        Log.i("sono nel metodo: ", "getAddress");

        List<Address> address = null;

        address = geocoder.getFromLocation(latitude, longitude, 1);

        Log.i("valore address: ", String.valueOf(address));

        if (address != null) {
            if (address.isEmpty()) {

                return null;
            }
            else
                {
                    if (address.size() > 0) {
                        StringBuffer checkAddress=new StringBuffer();
                        Address tmp=address.get(0);
                        for (int y=0;y<tmp.getMaxAddressLineIndex();y++)
                            checkAddress.append(tmp.getAddressLine(y)+"\n");

                        Log.i("valore checkAddress:", checkAddress.toString());

                        return checkAddress.toString();


                    }
                }
        }
        return null;

    }

    protected String getDate(){

        Log.i("sono nel metodo: ","getDate");

      // Date timestamp = new Date(mLastLocation.getTime());
        Date timestamp  = new Date(System.currentTimeMillis());

        Log.i("valore timestamp: ", String.valueOf(timestamp));

        return timestamp.toString();
    }


    protected boolean getGps()
    {
        Log.i("sono nel metodo: ", "getGps");

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);

        Log.i("isGpsEnabled: ", String.valueOf(isGpsEnabled));

        return isGpsEnabled;
    }

    protected boolean getNetwork()
    {
        Log.i("sono nel metodo: ", "getNetwork");

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        boolean isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        Log.i("isNetworkEnabled: ", String.valueOf(isNetworkEnabled));

        return isNetworkEnabled;
    }



    @Override
    protected void onStart() {

        Log.i("sono nel metodo: ", "onStart");

        super.onStart();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onResume() {

        Log.i("sono nel metodo: ", "onResume");

        super.onResume();

        geocoder = new Geocoder(this, Locale.getDefault());

        checkPlayServices();

        // Resuming the periodic location updates
        if (mGoogleApiClient.isConnected() && mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {

        Log.i("sono nel metodo: ", "onConnected");
        Log.i("lancio il metodo: ", "displayLocation");

        try {
            displayLocation();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (mRequestingLocationUpdates) {
            startLocationUpdates();
        }
    }

    @Override
    protected void onStop() {

        Log.i("sono nel metodo: ", "onStop");

        super.onStop();
        if (mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    protected void onPause() {

        Log.i("sono nel metodo: ", "onPause");

        super.onPause();
        stopLocationUpdates();
    }


    @Override
    public void onConnectionSuspended(int i) {

        Log.i("sono nel metodo: ", "onConnectionSuspended");

        mGoogleApiClient.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

        Log.i(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                + connectionResult.getErrorCode());

    }

    @Override
    public void onLocationChanged(Location location) {

        Log.i("sono nel metodo: ", "onLocationChanged");

        mLastLocation = location;

        Log.i("valore mLastLocation : ", String.valueOf(mLastLocation));

        Toast.makeText(getApplicationContext(), "Location changed!",
                Toast.LENGTH_SHORT).show();

        Log.i("lancio il metodo: ", "displayLocation");
        // Displaying the new location on UI
        try {
            displayLocation();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}