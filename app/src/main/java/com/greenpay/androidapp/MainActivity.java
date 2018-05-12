package com.greenpay.androidapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * The main activity of the app
 * shows a list of the stores that support GreenPay in your area
 * clicking a store will take you to an activity where you can pay in the store
 *
 * @author Yiftah Schlesinger
 * @version 1.0
 */
public class MainActivity extends AppCompatActivity
{
    ListView wifilist = null;
    TextView stateText; //a textview to display app state
    BroadcastReceiver wifiReceiver;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wifilist = findViewById(R.id.wifilist); //listview to display WiFi networks
        stateText = findViewById(R.id.stateText);
        final WifiManager mainWiFi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        if (!mainWiFi.isWifiEnabled()) //if wifi is disabled, enabling wifi
        {
            stateText.setText("Enabling Wifi...");
            mainWiFi.setWifiEnabled(true);
        }
        stateText.setText("Wifi Enabled");
        wifiReceiver = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
                {
                    List<ScanResult> mScanResults = mainWiFi.getScanResults();
                    stateText.setText("Found " + mScanResults.size() + " wifi networks");
                    ArrayList <String> list = new ArrayList();
                    for (int i = 0; i < mScanResults.size(); i++)
                    {
                        list.add(mScanResults.get(i).SSID);
                    }
                    wifilist.setAdapter(new ArrayAdapter <String> (MainActivity.this,
                            android.R.layout.simple_list_item_1, list));
                    stateText.setText("wifi networks found");
                }
            }
        };
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mainWiFi.startScan();
        stateText.setText("Scanning WiFi");
    }

    protected void onPause() {
        unregisterReceiver(wifiReceiver);
        super.onPause();
    }

    protected void onResume() {
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        super.onResume();
    }
}
