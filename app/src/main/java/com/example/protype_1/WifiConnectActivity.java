package com.example.protype_1;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class WifiConnectActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 1;
    WifiManager wifiManager;
    ListView wifiList;
    Button btn_search;
    WifiReceiver receiverWifi;
    boolean locationPermission;
    IntentFilter intentFilter;
    public static final String TAG = "WifiConnAct";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wifi_connect);
        initWork();
        initOnClicks();
        getLocationPermission();
        receiverWifi = new WifiReceiver(wifiManager, wifiList);
        intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION);
        registerReceiver(receiverWifi, intentFilter);
        wifiManager.startScan();
        if (locationPermission) {
            boolean worked = wifiManager.startScan();
            if (!worked) {
                Log.e(TAG, "Wifi Scan failed");
            }
        }

    }

    private void initWork() {
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        wifiList = findViewById(R.id.wifiNetworkListView);
        btn_search = findViewById(R.id.search);
        //debug

    }

    private void initOnClicks() {
        btn_search.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getLocationPermission();
                if (locationPermission) {
                    boolean worked = wifiManager.startScan();
                    if (!worked) {
                        Log.e(TAG, "Wifi Scan failed");
                    }
                }
            }
        });
        wifiList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ScanResult[] wifiArray = receiverWifi.getWifiArray();
                final ScanResult device = wifiArray[position];
                WifiConfiguration wifiConfig = new WifiConfiguration();
                wifiConfig.SSID = String.format("\"%s\"",device.SSID);
                wifiConfig.preSharedKey = String.format("\"%s\"", "dontsleep");
                int netId = wifiManager.addNetwork(wifiConfig);
                wifiManager.disconnect();
                wifiManager.enableNetwork(netId, true);
                wifiManager.reconnect();

                ConnectionChecker connectionChecker = new ConnectionChecker("b8:27:eb:d8:1f:44");
                runOnUiThread(connectionChecker);
            }
        });
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        receiverWifi = new WifiReceiver(wifiManager, wifiList);
        registerReceiver(receiverWifi, intentFilter);
        wifiManager.startScan();
    }

    @Override
    protected void onPause() {
        //unregisterReceiver(receiverWifi);
        super.onPause();
    }

    public void getLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Should we show an explanation?
            locationPermission = false;
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Show an explanation to the user
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION);
            }
        } else {
            // Permission has already been granted
            locationPermission = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == MY_PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                locationPermission = true;
            } else {
                locationPermission = false;
            }
        }
    }

    public void startCommunicationActivity() {
        unregisterReceiver(receiverWifi);

        Intent nextActivity = new Intent(this, CommunicationActivity.class);
        startActivity(nextActivity);
    }

    private class ConnectionChecker implements Runnable {
        String targetBSSID;
        WifiInfo wifiInfo;
        public ConnectionChecker(String targetBSSID) {
            this.targetBSSID = targetBSSID;

        }

        @Override
        public void run() {
            boolean stop = false;
            int timeout = 100;
            int counter = 0;
            while(!stop) {
                wifiInfo = wifiManager.getConnectionInfo();
                String bssid = wifiInfo.getBSSID();
                String ssid = wifiInfo.getSSID();
                Log.i(TAG, "BSSID: " + bssid);
                Log.i(TAG, "SSID: " + ssid);
                if(bssid != null ){// && bssid.equals(targetBSSID)) {
                    stop = true;
                    startCommunicationActivity();
                    break;
                }
                counter++;
                if(counter > timeout) {
                    Log.i(TAG, "Timed out, exiting thread... ");
                    stop = true;
                }
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

}
