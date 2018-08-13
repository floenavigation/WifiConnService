package com.hfad.wificonnservice;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.IBinder;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class WiFiService extends Service {

    private WifiManager wifiManager;
    private WifiConfiguration wifiConfig;
    private int networkID;
    private String ssid;
    private List<ScanResult> results;
    private BroadcastReceiver wifiReceiver;
    private ArrayList<String> arrayList = new ArrayList<>();

    public WiFiService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }


    @Override
    public void onCreate() {
        super.onCreate();

        //Enable Wifi
        wifiManager = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);

        try{
            if (!wifiManager.isWifiEnabled()) {
                //Toast.makeText(this, "WiFi is disabled ... We need to enable it", Toast.LENGTH_LONG).show();
                wifiManager.setWifiEnabled(true);
            }
        }catch (Exception e){
            Toast.makeText(this, "wifiManager.isWifiEnabled() returned null", Toast.LENGTH_LONG).show();
        }



        //For Network Scanning
        ssid = "AIS Class B";
        wifiNetworkConfig();

        //To print the network connections



        //Receiver
        wifiReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                results = wifiManager.getScanResults();
                //Toast.makeText(getApplicationContext(),"scan results received!!", Toast.LENGTH_LONG).show();
                unregisterReceiver(this);
                arrayList.clear();

                if(!results.isEmpty()) {
                    Toast.makeText(getApplicationContext(),"scan results received!!", Toast.LENGTH_LONG).show();

                    for (ScanResult scanResult : results) {
                        arrayList.add(scanResult.SSID + " - " + scanResult.capabilities);

                        //Broadcast to MainActivity
                        Intent intent_MainAct = new Intent("wifi_updates");
                        intent_MainAct.putExtra("scanlist", arrayList);
                        sendBroadcast(intent_MainAct);

                        Toast.makeText(getApplicationContext(), scanResult.SSID, Toast.LENGTH_LONG).show();

                        if(scanResult.SSID.trim().equals(ssid)){
                            Toast.makeText(getApplicationContext(), "matched", Toast.LENGTH_LONG).show();
                            wifiManager.disconnect();
                            wifiManager.enableNetwork(networkID, true);
                            boolean success = wifiManager.reconnect();
                            if(success) {
                                Toast.makeText(getApplicationContext(), "Connected to AIS", Toast.LENGTH_LONG).show();
                                break;
                            }else {
                                wifiManager.reassociate();
                                Toast.makeText(getApplicationContext(), "Not connected to AIS", Toast.LENGTH_LONG).show();
                                break;
                            }
                        }
                    }
                }
                else{
                    Toast.makeText(getApplicationContext(),"Empty List!!", Toast.LENGTH_LONG).show();
                    //registerWifi();
                    //startScan();
                }
            }
        };

        registerWifi();
        startScan();
    }

    private void wifiNetworkConfig() {

        wifiConfig = new WifiConfiguration();
        wifiConfig.SSID = String.format("\"%s\"", ssid);
        wifiConfig.allowedKeyManagement.set(WifiConfiguration.KeyMgmt.NONE);
        networkID = wifiManager.addNetwork(wifiConfig);
    }


    private void registerWifi() {
        arrayList.clear();
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

    }

    private void startScan() {

        wifiManager.startScan();
        Toast.makeText(this, "Scanning WiFi ...", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(wifiReceiver);
    }
}
