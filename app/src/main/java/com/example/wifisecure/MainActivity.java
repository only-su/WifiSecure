package com.example.wifisecure;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.gson.Gson;

public class MainActivity extends AppCompatActivity {

    private SharedPreferences data;
    private WifiManager wifiManager;

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
        wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        data = getPreferences(Context.MODE_PRIVATE);
    }

    public void buttonStoreSSID(View view){
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        SharedPreferences.Editor edit = data.edit();
        Gson gson = new Gson();
        String json = gson.toJson(wifiInfo);
        edit.putString(wifiInfo.getSSID(), json);
        edit.apply();

    }

    @SuppressLint("SetTextI18n")
    public void buttonCheckSSID(View view){
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if(data.contains(wifiInfo.getSSID())) {
            Gson gson = new Gson();
            String json = data.getString(wifiInfo.getSSID(), "");
            WifiInfo obj = gson.fromJson(json, WifiInfo.class);
            textView.setText(wifiInfo.toString());
        }else{
            textView.setText("SSID não encontrado: se tem confiança nessa rede " +
                                "clique em store para armazená-la");
        }
    }

    public void buttonClearData(View view) {
        SharedPreferences.Editor edit = data.edit();
        edit.clear();
        edit.apply();
    }
}