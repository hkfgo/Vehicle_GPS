package com.rev.xi.rev_gps;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.location.Location;
import android.os.Handler;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.util.Date;
import java.util.Set;
import java.util.UUID;

public class MapsActivity extends FragmentActivity implements ConnectionCallbacks, OnConnectionFailedListener, LocationListener {

    private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private String TAG = "REV_GPS.MapActivity";
    private Double longitude;
    private Double latitude;
    LocationRequest mLocationRequest;
    private final static int PLAY_SERVICES_RESOLUTION_REQUEST = 1000;

    private OutputStream bluetoothOutput;
    private InputStream bluetoothInput;
    private boolean stopWorker = false;

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    private GoogleApiClient mGoogleApiClient;
    private Location mLastLocation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
//        if(checkPlayServices()){
//            mGoogleApiClient = new GoogleApiClient.Builder(this)
//                        .addConnectionCallbacks(this)
//                        .addOnConnectionFailedListener(this)
//                        .addApi(LocationServices.API)
//                        .build();
//            mLocationRequest = new LocationRequest();
//            mLocationRequest.setInterval(5000);
//            mLocationRequest.setFastestInterval(1000);
//            mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
//        }
        Button connectBluetoothButton = (Button) findViewById(R.id.bluetooth_connect);
        connectBluetoothButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                connectArduinoBluetooth();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        //mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        //mGoogleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        LocationServices.FusedLocationApi.requestLocationUpdates(
                mGoogleApiClient, mLocationRequest, this);
        mLastLocation = LocationServices.FusedLocationApi.getLastLocation(
                mGoogleApiClient);
        updateFields();
    }

    private void updateFields() {
        longitude = mLastLocation.getLongitude();
        latitude = mLastLocation.getLatitude();
        String time = DateFormat.getTimeInstance().format(new Date());
        Log.d(TAG, "current location: " + longitude + " " + latitude + " at " + time);
        ((TextView) findViewById(R.id.latitude)).setText("latitude: " + latitude.toString());
        ((TextView) findViewById(R.id.longitude)).setText("longitude: " + longitude.toString());
        ((TextView) findViewById(R.id.time)).setText("time: " + time);
        ((TextView) findViewById(R.id.speed)).setText("speed: " + mLastLocation.getSpeed() + " m/s");
        ((TextView) findViewById(R.id.accuracy)).setText("accuracy: " + mLastLocation.getAccuracy() + " m");
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call {@link #setUpMap()} once when {@link #mMap} is not null.
     * <p/>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p/>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();
            // Check if we were successful in obtaining the map.
            if (mMap != null) {
                setUpMap();
            }
        }
    }

    /**
     * This is where we can add markers or lines, add listeners or move the camera. In this case, we
     * just add a marker near Africa.
     * <p/>
     * This should only be called once and when we are sure that {@link #mMap} is not null.
     */
    private void setUpMap() {
        mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title("Marker"));
    }

    @Override
    public void onLocationChanged(Location location) {
        mLastLocation = location;
        updateFields();
    }

    private boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(this);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultCode)) {
                GooglePlayServicesUtil.getErrorDialog(resultCode, this,
                        PLAY_SERVICES_RESOLUTION_REQUEST).show();
            } else {
                Toast.makeText(getApplicationContext(),
                        "This device is not supported.", Toast.LENGTH_LONG)
                        .show();
                finish();
            }
            return false;
        }
        return true;
    }

    private void connectArduinoBluetooth() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if(!adapter.isEnabled()) {
            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBluetooth, 0);
        }
        Set<BluetoothDevice> pairedDevices = adapter.getBondedDevices();
        BluetoothDevice pairedDevice = null;
        if(pairedDevices.size() > 0) {
            for(BluetoothDevice device : pairedDevices) {
                Log.d("device name", device.getName());
                if(device.getName().equals("HC-06")) { //NAME OF BLUETOOTH DEVICE HERE
                    pairedDevice = device;
                    break;
                }
            }
        }
        if (pairedDevice == null) {
            ((TextView) findViewById(R.id.arduino_input)).setText("failed to find paired device");
            return;
        }
        ((TextView) findViewById(R.id.arduino_input)).setText("found paired device");
        UUID uuid = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb"); //Standard SerialPortService ID
        try {
            BluetoothSocket socket = pairedDevice.createRfcommSocketToServiceRecord(uuid);
            socket.connect();
            bluetoothOutput = socket.getOutputStream();
            bluetoothInput = socket.getInputStream();
            listenForData();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void listenForData() {
        System.out.println("listening");
        final Handler handler = new Handler();
        Thread workerThread = new Thread(new Runnable() {
            public void run() {
                byte[] readBuffer = new byte[1024];
                int readBufferPosition = 0;
                while(!Thread.currentThread().isInterrupted() && !stopWorker) {
                    int bytesAvailable = 0;
                    try {
                        bytesAvailable = bluetoothInput.available();
                        if(bytesAvailable > 0) {
                            byte[] packetBytes = new byte[bytesAvailable];
                            bluetoothInput.read(packetBytes);

                            for(int i = 0; i < bytesAvailable; i++) {
                                byte b = packetBytes[i];
                                System.out.println(b);
                                if(b == '\n') {  // delimiter character to be sent at the end of every arduino command
                                    byte[] encodedBytes = new byte[readBufferPosition];
                                    System.arraycopy(readBuffer, 0, encodedBytes, 0, encodedBytes.length);
                                    final String data = new String(encodedBytes, "US-ASCII");
                                    readBufferPosition = 0;
                                    // The variable data now contains our full command
                                    // handler used to display data back on UI thread
                                    handler.post(new Runnable() {
                                        public void run() {
                                            ((TextView) findViewById(R.id.arduino_input)).setText(data);
                                        }
                                    });
                                } else {
                                    readBuffer[readBufferPosition++] = b;
                                }
                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        workerThread.start();
    }
}
