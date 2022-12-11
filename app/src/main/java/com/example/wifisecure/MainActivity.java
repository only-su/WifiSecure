package com.example.wifisecure;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.NetworkRequest;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;

import java.time.Instant;
import java.util.Date;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final long ONE_YEAR = 31_556_926_000L;

    protected class mWifiInfo {
        public String SSID;
        public int securityType;

        public double lastConnLat;
        public double lastConnLon;

        public Date lastConnTime;

        mWifiInfo(WifiInfo wifiInfo, Location loc) {
            this.SSID = wifiInfo.getSSID();
            this.securityType = wifiInfo.getCurrentSecurityType();

            this.lastConnLat = loc.getLatitude();
            this.lastConnLon = loc.getLongitude();

            this.lastConnTime = new Date();
        }

        @NonNull
        @Override
        public java.lang.String toString() {

            return "{" +
                    "SSID=" + SSID +
                    ", securityType=" + securityType +
                    ", latitude=" + lastConnLat +
                    ", longitude=" + lastConnLon +
                    ", lastConnTime=" + lastConnTime +
                    '}';
        }
    }

    private SharedPreferences data;
    private WifiManager wifiManager;

    private Location currLocation;

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        data = getPreferences(Context.MODE_PRIVATE);

        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                },
                PackageManager.PERMISSION_GRANTED
        );

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000 * 6, 10, this);
    }

    @Override
    public void onLocationChanged(Location location) {
        currLocation = location;
    }

    @Override
    public void onProviderDisabled(String provider) {
        Log.d("Latitude","disable");
    }

    @Override
    public void onProviderEnabled(String provider) {
        Log.d("Latitude", "enable");
    }

    public void buttonStoreSSID(View view) {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        SharedPreferences.Editor edit = data.edit();

        mWifiInfo CurrWifi;
        CurrWifi = new mWifiInfo(
                wifiInfo,
                currLocation
        );

        Gson gson = new Gson();
        String json = gson.toJson(CurrWifi);
        edit.putString(wifiInfo.getSSID(), json);
        edit.apply();

    }

    public void buttonCheckSSID(View view){
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if(data.contains(wifiInfo.getSSID())) {
            Gson gson = new Gson();
            String json = data.getString(wifiInfo.getSSID(), "");
            mWifiInfo obj = gson.fromJson(json, mWifiInfo.class);
            textView.setText(obj.toString());
            textView.append("\n{"+currLocation.getLatitude()+", "+currLocation.getLongitude()+"}");

            if( (new Date()).getTime() - obj.lastConnTime.getTime() > ONE_YEAR ){
                textView.append("\n\nALERTA: Revise sua conexão, ja faz 1 ano desde que se conectou a esse SSID");
            }
            if( obj.securityType != wifiInfo.getCurrentSecurityType() ){
                textView.append("\n\nALERTA: Revise sua conexão, o protocolo de segurança foi alterado");
            }

            float[] distance = new float[3];
            Location.distanceBetween(currLocation.getLatitude(), currLocation.getLongitude(), obj.lastConnLat, obj.lastConnLon, distance);

            if( distance[0] > 2000){
                textView.append("\n\nALERTA: Revise sua conexão, sua ultima conexão foi em uma posição geografica consideravelmente distante");
            }
        }else{
            textView.setText("SSID não encontrado: se tem confiança nessa rede " +
                                "clique em store para armazená-la");
        }
    }

    public void buttonClearData(View view) {
        SharedPreferences.Editor edit = data.edit();
        edit.clear();
        edit.apply();
        textView.setText("Hello World");
    }
}