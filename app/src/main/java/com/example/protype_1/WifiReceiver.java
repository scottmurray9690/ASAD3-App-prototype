package com.example.protype_1;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.telephony.TelephonyScanManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class WifiReceiver extends BroadcastReceiver {
    WifiManager wifiManager;
    StringBuilder sb;
    ListView wifiListView;
    ScanResult[] wifiArray;
    public WifiReceiver( WifiManager wifiManager, ListView wifiListView) {
        this.wifiManager = wifiManager;
        this.wifiListView = wifiListView;
    }
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        if (WifiManager.SCAN_RESULTS_AVAILABLE_ACTION.equals(action)) {
            sb = new StringBuilder();
            List<ScanResult> wifiList = wifiManager.getScanResults();
            wifiArray = new ScanResult[wifiList.size()];
            ArrayList<String> deviceList = new ArrayList<>();
            int index = 0;
            for (ScanResult scanResult : wifiList) {
                wifiArray[index] = scanResult;
                sb.append("\n").append(scanResult.SSID + " - " + scanResult.BSSID);
                deviceList.add(scanResult.SSID + " - " + scanResult.BSSID);
                index++;
            }
            ArrayAdapter arrayAdapter = new ArrayAdapter(context, android.R.layout.simple_list_item_1, deviceList.toArray());
            wifiListView.setAdapter(arrayAdapter);
        }
    }

    public ScanResult[] getWifiArray() {
        return wifiArray;
    }
}
