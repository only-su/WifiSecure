package com.example.wifisecure;

import android.app.AlertDialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.DialogInterface;
import android.net.ConnectivityManager;
import android.net.Network;
import android.util.Log;
import android.view.WindowManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.FragmentManager;

import java.util.Random;

public class WifiObserver {
    private final Context context;
    private final NotificationManagerCompat notificationManager;
    private final String CHANNEL_ID = "10001";

    public WifiObserver(Context context ) {
        this.context = context;

        this.notificationManager = NotificationManagerCompat.from(context);

        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "Test",
                NotificationManager.IMPORTANCE_HIGH);
        notificationManager.createNotificationChannel(channel);
    }

    public void registerNetworkObserver() {
        try {
            ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            connectivityManager.registerDefaultNetworkCallback(
                    new ConnectivityManager.NetworkCallback(){
                       @Override
                       public void onAvailable(Network network) {

                           Log.e("WifiManager", "Nova rede conectada");

                           MainActivity mainActivity = (MainActivity) context;

                           int check_res = mainActivity.checkWifiSSID();
                           String SSID = mainActivity.getWifiSSID();

                           if( check_res == 0 ) {
                               Log.e("WifiSecure", "SSID não encontrada, armazenando informações");
                               mainActivity.storeWifiSSID();
                               mainActivity.showNotification("WifiSecure", "Informações sobre nova rede '" + SSID + "' armazenadas!");
                           } else if(check_res == 1) {
                               Log.e("WifiSecure","Conexão segura, atualizando infromações!");
                               mainActivity.storeWifiSSID();
                           } else if(check_res == -1) {
                               Log.e("WifiSecure","Revise sua conexão, ja faz mais de 1 ano desde que se conectou a esse SSID");
                               mainActivity.showNotification("WifiSecure - Revise sua conexão!", "Já faz mais de 1 ano desde que se conectou a '" + SSID + "'!");
                               mainActivity.showUnsafeWifiDialog("Já faz mais de 1 ano desde que se conectou a '" + SSID + "'!\nDeseja continuar conectado mesmo assim?");
                           } else if (check_res == -2) {
                               Log.e("WifiSecure","Revise sua conexão, o protocolo de segurança foi alterado");
                               mainActivity.showNotification("WifiSecure - Revise sua conexão!", "O protocolo de segurança foi alterado desde a última vez que se conectou a '" + SSID + "'!");
                               mainActivity.showUnsafeWifiDialog("O protocolo de segurança foi alterado desde a última vez que se conectou a '" + SSID + "'!\nDeseja continuar conectado mesmo assim?");
                           } else if (check_res == -3) {
                               Log.e("WifiSecure","Revise sua conexão, sua ultima conexão foi em uma posição geografica consideravelmente distante");
                               mainActivity.showNotification("WifiSecure - Revise sua conexão!", "Sua ultima conexão com '" + SSID + "' foi em uma posição geográfica consideravelmente distante!");
                               mainActivity.showUnsafeWifiDialog("Sua ultima conexão com '" + SSID + "' foi em uma posição geográfica consideravelmente distante!\nDeseja continuar conectado mesmo assim?");
                           }

                           Log.e("WifiManager", "Fim do código de notificação");

                       }
                   }

            );
        }catch (Exception e){
            Log.e("WifiManager", e.getMessage());
        }
    }
}
