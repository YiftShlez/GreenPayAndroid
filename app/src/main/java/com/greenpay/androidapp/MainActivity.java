package com.greenpay.androidapp;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

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
    ListView wifilist = null; //a listview to display available stores
    TextView stateText; //a textview to display app state
    BroadcastReceiver wifiReceiver; //a receiver to detect new WiFi networks
    boolean wifiStateOriginal; //the original state of the WiFi (on/off)
    WifiManager mainWiFi; //The object that controls the WiFi
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        wifilist = findViewById(R.id.wifilist);
        stateText = findViewById(R.id.stateText);
        //get the wifi service and initialize the wifi manager
        mainWiFi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        //set the wifiStateOriginal to the current state
        //to turn off WiFi when leaving the app, in case it was off
        wifiStateOriginal = mainWiFi.isWifiEnabled();
        if (!mainWiFi.isWifiEnabled()) //if wifi is disabled, enabling wifi
        {
            stateText.setText("Enabling Wifi...");
            mainWiFi.setWifiEnabled(true);
        }
        stateText.setText("Wifi Enabled");
        wifiReceiver = new BroadcastReceiver()
        {
            @Override
            //this method is called every time a scan result is done
            public void onReceive(Context context, Intent intent)
            {
                //check if the action was the wifi scan results
                if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION))
                {
                    //the list of the wifi networks
                    List<ScanResult> mScanResults = mainWiFi.getScanResults();
                    stateText.setText("Found " + mScanResults.size() + " wifi networks");
                    ArrayList <String> list = new ArrayList();
                    for (int i = 0; i < mScanResults.size(); i++)
                    {
                        list.add(mScanResults.get(i).SSID);
                    }
                    //add all the wifi networks to the listview
                    wifilist.setAdapter(new ArrayAdapter <String> (MainActivity.this,
                            android.R.layout.simple_list_item_1, list));
                    stateText.setText("wifi networks found");
                }
            }
        };
        //register the BroadcastReceiver
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        //a receiver for checking if the wifi state was changed
        BroadcastReceiver onWifiStateChanged = new BroadcastReceiver()
        {
            @Override
            public void onReceive(Context context, Intent intent)
            {
                if (intent.getAction().equals(WifiManager.WIFI_STATE_CHANGED_ACTION))
                {
                    if (mainWiFi.isWifiEnabled()) //if WiFi was off and now is enabled
                    {
                        //register the receiver
                        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                    }
                    else //if wifi was on and now id disabled
                    {
                        //stop the receiver
                        unregisterReceiver(wifiReceiver);
                        //inform the user that the WiFi must be on
                        Toast.makeText(context, "Service cannot work without WiFi", Toast.LENGTH_LONG).show();
                        stateText.setText("WiFi is disabled");
                    }
                }
            }
        };
        registerReceiver(onWifiStateChanged, new IntentFilter(WifiManager.WIFI_STATE_CHANGED_ACTION));
        //start the wifi scan
        mainWiFi.startScan();
        stateText.setText("Scanning WiFi");
    }


    protected void onPause() {
        //stop scanning WiFi when leaving the app
        unregisterReceiver(wifiReceiver);
        //if the WiFi was off before the app was opened, turn it off
        if (!wifiStateOriginal)
            mainWiFi.setWifiEnabled(false);
        //stop the app
        super.onPause();
    }

    protected void onResume() {
        //register the receiver back
        registerReceiver(wifiReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        mainWiFi.startScan(); //start a new scan
        wifiStateOriginal = mainWiFi.isWifiEnabled(); //check if wifi is enabled
        if (!wifiStateOriginal)
            mainWiFi.setWifiEnabled(true); //enabling wifi if it was disabled
        super.onResume(); //resume the app
    }
}
