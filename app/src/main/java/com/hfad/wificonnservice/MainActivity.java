package com.hfad.wificonnservice;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ArrayList<String> arrayList = new ArrayList<>();
    private ArrayAdapter adapter;
    private ListView listView;
    private Button buttonScan;
    private BroadcastReceiver broadcastReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //Setting all the views
        buttonScan = findViewById(R.id.scanBtn);
        listView = findViewById(R.id.wifiList);
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, arrayList);
        listView.setAdapter(adapter);

        //Register BroadcastReceiver
        //LocalBroadcastManager.getInstance(MainActivity.this).registerReceiver(broadcastReceiver, new IntentFilter("wifi_updates"));

        //Req permissions
        runtime_permissions();

    }

    private void enableButton() {
        buttonScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Start the Service
                Intent intent = new Intent(getApplicationContext(), WiFiService.class);
                startService(intent);

            }
        });

    }


    private void runtime_permissions() {
        if(Build.VERSION.SDK_INT >= 23){ /*&& (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_WIFI_STATE)!=
                PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(this, Manifest.permission.CHANGE_WIFI_STATE)!=
                PackageManager.PERMISSION_GRANTED) && (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_NETWORK_STATE)!=
                PackageManager.PERMISSION_GRANTED)) {*/

            requestPermissions(new String[]{Manifest.permission.ACCESS_WIFI_STATE,
                    Manifest.permission.CHANGE_WIFI_STATE, Manifest.permission.ACCESS_NETWORK_STATE,
                    Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.INTERNET}, 100);
            //Toast.makeText(getApplicationContext(),"requestPerm", Toast.LENGTH_LONG).show();
        }
        else {
            Toast.makeText(getApplicationContext(), "sdk<23", Toast.LENGTH_LONG).show();
            //scanWifi();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode == 100){
            if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED
                    && grantResults[2] == PackageManager.PERMISSION_GRANTED && grantResults[3] == PackageManager.PERMISSION_GRANTED
                    && grantResults[4] == PackageManager.PERMISSION_GRANTED && grantResults[5] == PackageManager.PERMISSION_GRANTED){
                //Toast.makeText(getApplicationContext(),"permGranted", Toast.LENGTH_LONG).show();
                enableButton();

            }else{
                //Toast.makeText(getApplicationContext(),"permnotGranted", Toast.LENGTH_LONG).show();
                runtime_permissions();
            }
        }
    }


    @Override
    protected void onResume() {
        super.onResume();
        if(broadcastReceiver == null){
            broadcastReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    //coord.append("\n" + intent.getExtras().get("coordinates"));
                    arrayList.clear();
                    arrayList.addAll(intent.getStringArrayListExtra("scanlist"));
                    adapter.notifyDataSetChanged();
                }
            };
        }
        registerReceiver(broadcastReceiver, new IntentFilter("wifi_updates"));
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(broadcastReceiver != null){
            unregisterReceiver(broadcastReceiver);
            //broadcastReceiver = null;
        }
    }
}
