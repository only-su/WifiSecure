package com.example.wifisecure;

import android.Manifest;
import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.wifi.SupplicantState;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;

import java.util.Date;

public class MainActivity extends AppCompatActivity implements LocationListener {

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
    private AlertDialog dialogOverlay;
    private AlertDialog dialogLocation;

    private final long DAY = 86_400_000L;
    private int timeConstraint = 365;
    private int spaceConstraint = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        textView = findViewById(R.id.textView);
        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        data = getPreferences(Context.MODE_PRIVATE);

        NumberPicker npSpaceValue = findViewById(R.id.npSpaceConstraint);
        npSpaceValue.setMinValue(1);
        npSpaceValue.setMaxValue(100000);
        npSpaceValue.setValue(timeConstraint);
        npSpaceValue.setOnValueChangedListener((picker, oldVal, newVal) -> {
            timeConstraint = newVal;
        });

        NumberPicker npTimeValue = findViewById(R.id.npTimeConstraint);
        npTimeValue.setMinValue(1);
        npTimeValue.setMaxValue(100000);
        npTimeValue.setValue(spaceConstraint);
        npTimeValue.setOnValueChangedListener((picker, oldVal, newVal) -> {
            spaceConstraint = newVal;
        });

        ActivityCompat.requestPermissions(this,
                new String[]{
                        Manifest.permission.ACCESS_WIFI_STATE,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.SYSTEM_ALERT_WINDOW,
                        Manifest.permission.POST_NOTIFICATIONS
                },
                PackageManager.PERMISSION_GRANTED
        );

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle("Permissão de backgorund location")
                    .setMessage("Para que WiFiSecure funcione corretamente, é preciso fornecer permissão de acesso a localização em segundo plano!")
                    .setPositiveButton("Conceder permissão", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        intent.setData(Uri.fromParts("package", getPackageName(), null));
                        startActivity(intent);
                    });

            dialogLocation = builder.create();
            if (dialogLocation != null && !dialogLocation.isShowing()) dialogLocation.show();
        }

        if( !Settings.canDrawOverlays(this) ) {
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle("Permissão de overlay")
                    .setMessage("Para que WiFiSecure funcione corretamente, é preciso fornecer permissão de overlay sobre outros aplicativos!")
                    .setPositiveButton("Conceder permissão", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                        intent.setData(Uri.fromParts("package", getPackageName(), null));
                        startActivity(intent);
                    });

            dialogOverlay = builder.create();
            dialogOverlay.show();
        }

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000 * 6, 10, this);

        WifiObserver wifiObserver = new WifiObserver(this);
        wifiObserver.registerNetworkObserver();
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

    @Override
    protected void onResume() {
        super.onResume();

        if( !Settings.canDrawOverlays(this) ) {
            if (!dialogOverlay.isShowing()) return;

            if (dialogOverlay == null){
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle("Permissão de overlay")
                        .setMessage("Para que WiFiSecure funcione corretamente, é preciso fornecer permissão de overlay sobre outros aplicativos!")
                        .setPositiveButton("Conceder permissão", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                            intent.setData(Uri.fromParts("package", getPackageName(), null));
                            startActivity(intent);
                        });
                dialogOverlay = builder.create();
            }

            dialogOverlay.show();
        }

        if(ContextCompat.checkSelfPermission(this,Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (!dialogLocation.isShowing()) return;

            if(dialogLocation == null) {
                AlertDialog.Builder builder = new AlertDialog.Builder(this)
                        .setTitle("Permissão de backgorund location")
                        .setMessage("Para que WiFiSecure funcione corretamente, é preciso fornecer permissão de acesso a localização em segundo plano!")
                        .setPositiveButton("Conceder permissão", (dialog, which) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.fromParts("package", getPackageName(), null));
                            startActivity(intent);
                        });
                dialogLocation = builder.create();
            }

            dialogLocation.show();
        }
    }

    public void buttonStoreSSID(View view) {
        storeWifiSSID();
    }

    public void buttonCheckSSID(View view){
        int check_res = checkWifiSSID();

        if(check_res == 0) {
            textView.setText("SSID não encontrado: se tem confiança nessa rede " +
                    "clique em store para armazená-la");
        } else if (check_res == -4) {
            textView.setText("Para verificar, primeiro conecte-se a uma rede :)");
        } else {
            WifiInfo wifiInfo = wifiManager.getConnectionInfo();
            Gson gson = new Gson();
            String json = data.getString(wifiInfo.getSSID(), "");
            Log.e("AQUIII ",wifiInfo.getSSID());
            mWifiInfo obj = gson.fromJson(json, mWifiInfo.class);
            textView.setText(obj.toString());

            if(check_res == -1) {
                textView.append("\n\nALERTA: Revise sua conexão, ja faz 1 ano desde que se conectou a esse SSID");
            } else if (check_res == -2) {
                textView.append("\n\nALERTA: Revise sua conexão, o protocolo de segurança foi alterado");
            } else if (check_res == -3) {
                textView.append("\n\nALERTA: Revise sua conexão, sua ultima conexão foi em uma posição geografica consideravelmente distante");
            }
        }
    }

    public String getWifiSSID() {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if(wifiInfo != null)
            return wifiInfo.getSSID();

        return "";
    }

    public int checkWifiSSID() {
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        if(wifiInfo == null || (wifiInfo.getSupplicantState() != SupplicantState.COMPLETED)) return -4;

        if(data.contains(wifiInfo.getSSID())) {
            Gson gson = new Gson();
            String json = data.getString(wifiInfo.getSSID(), "");
            mWifiInfo obj = gson.fromJson(json, mWifiInfo.class);
            Log.e("WifiSecure", "SSID: " + obj.toString());

            if( (new Date()).getTime() - obj.lastConnTime.getTime() > (timeConstraint * DAY)){
                return -1;
            }
            if( obj.securityType != wifiInfo.getCurrentSecurityType() ){
                return -2;
            }

            if( currLocation != null ) {
                float[] distance = new float[3];
                Location.distanceBetween(currLocation.getLatitude(), currLocation.getLongitude(), obj.lastConnLat, obj.lastConnLon, distance);

                if (distance[0] > spaceConstraint) {
                    return -3;
                }
            }
            return 1;
        }else{
            return 0;
        }
    }

    public void storeWifiSSID() {
        if(currLocation == null) {
            Log.e("WifiSecure", "Erro ao armazernar informações!");
            return;
        }

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
        Log.e("WifiSecure", "Informações armazenadas!");
    }

    public void buttonClearData(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Limpar dados")
                .setMessage("Deseja limpar todos os dados sobre rede WiFi armazenados pelo aplicativo?")
                .setPositiveButton("Limpar", (dialog, which) -> {
                    clearData();
                })
                .setNegativeButton("Cancelar", (dialog, which) -> {});

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void clearData() {
        SharedPreferences.Editor edit = data.edit();
        edit.clear();
        edit.apply();
        textView.setText("Hello World");
    }

    public void showNotification(String title, String text) {
        String CHANNEL_ID = "wifiSecure_channel";

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(getApplicationContext());

        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), "default")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(text);

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "WifiSecure",
                NotificationManager.IMPORTANCE_DEFAULT);
        builder.setChannelId(CHANNEL_ID);
        notificationManager.createNotificationChannel(channel);

        notificationManager.notify((int) System.currentTimeMillis(), builder.build());
    }

    public void showUnsafeWifiDialog(String msg) {
        if(!Settings.canDrawOverlays(this)) return;

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Rede WiFi insegura!")
                .setMessage(msg)
                .setPositiveButton("Continuar", (dialog, which) -> {})
                .setNegativeButton("Desconectar", (dialog, which) -> {
                    startActivity(new Intent(Settings.ACTION_WIFI_SETTINGS));
                });

        AlertDialog dialog = builder.create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        dialog.show();
    }
}