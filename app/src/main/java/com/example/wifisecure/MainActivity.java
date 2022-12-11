package com.example.wifisecure;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.gson.Gson;

import java.time.Instant;
import java.util.Date;

public class MainActivity extends AppCompatActivity {

    private class mWifiInfo {
        public String SSID;
        public Date lastConnTime;
        public Location lastConnLocation;

        mWifiInfo(String SSID, Location currLocation) {
            this.SSID = SSID;
            this.lastConnTime = new Date();
            this.lastConnLocation = currLocation;
        }
    }

    private SharedPreferences data;
    private WifiManager wifiManager;

    private FusedLocationProviderClient fusedLocationClient;

    private TextView textView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION
                },
                PackageManager.PERMISSION_GRANTED
        );

        textView = findViewById(R.id.textView);
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        data = getPreferences(Context.MODE_PRIVATE);

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

    }

    public void buttonStoreSSID(View view) {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        SharedPreferences.Editor edit = data.edit();

        final Location[] currLocation = new Location[1];

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return;
        }

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, new OnSuccessListener<Location>() {
                    @Override
                    public void onSuccess(Location location) {
                        // Got last known location. In some rare situations this can be null.
                        if (location != null) {
                            currLocation[0] = location;
                        }
                    }
                });

        mWifiInfo CurrWifi = new mWifiInfo(
                                    wifiInfo.getSSID(),
                                    currLocation[0] //TODO como q localização funciona?
                                );

        Gson gson = new Gson();
        String json = gson.toJson(CurrWifi);

        edit.putString(wifiInfo.getSSID(), json);
        edit.apply();

    }

    @SuppressLint("SetTextI18n")
    public void buttonCheckSSID(View view){
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if(data.contains(wifiInfo.getSSID())) {
            Gson gson = new Gson();
            String json = data.getString(wifiInfo.getSSID(), "");
            mWifiInfo obj = gson.fromJson(json, mWifiInfo.class);
            textView.setText(json);
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