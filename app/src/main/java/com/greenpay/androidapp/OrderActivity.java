package com.greenpay.androidapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.xmlpull.v1.XmlPullParserException;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import static com.greenpay.androidapp.MainActivity.TAG;

//this activity shows up after the camera detects a barcode of greenpay
public class OrderActivity extends AppCompatActivity
{
    //a text view that shows the details of the store (name, city, phoneno, etc)
    TextView orderDetails;

    //a textview that shows the receipt of the order
    TextView orderContent;

    //a client for server connection
    OkHttpClient client;

    //the id of the order
    int orderId;

    //the id of the store
    int storeId;

    //the price of the order
    double price;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);
        //initialize the ui elements
        orderDetails = findViewById(R.id.order_details);
        orderContent = findViewById(R.id.order_content);
        //get the details from the activity's intent
        Intent intent = getIntent();
        orderId = intent.getIntExtra("orderId", 0);
        storeId = intent.getIntExtra("storeId", 0);
        String storeName = intent.getStringExtra("storeName");
        String storeCity = intent.getStringExtra("storeCity");
        String storeEmail = intent.getStringExtra("storeEmail");
        price = intent.getDoubleExtra("price", 0.0);
        String content = intent.getStringExtra("content");
        String detailsText = "order #" + orderId + "\n" +
                storeName + "\nstore #" + storeId + "\n" +
                "city: " + storeCity + "\n" +
                "email address: " + storeEmail + "\n" +
                "price: " + price + "₪\n";
        //display the details in the ui elements
        orderDetails.setText(detailsText);
        orderContent.setText(content);

        client = new OkHttpClient();
    }

    /**
     * an onclick for the pay button
     * contacts the server and requests to pay
     * @param view the button
     */
    public void pay (View view)
    {
        Request request = new Request.Builder()
                .url(HttpUrl.parse("http://10.0.0.13:34526/GreenPay")
                        .newBuilder()
                        .addQueryParameter("request", "pay")
                        .addQueryParameter("orderId", "" + orderId)
                        .addQueryParameter("storeId", "" + storeId)
                        .addQueryParameter("price", "" + price)
                        .build().toString()).build();
        client.newCall(request).enqueue(
                new Callback()
                {
                    @Override
                    public void onFailure(Call call, IOException e)
                    {
                        Log.e(TAG, "Failed to communicate with server");
                        Log.e(TAG, Log.getStackTraceString(e));
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException
                    {
                        if (!response.isSuccessful())
                        {
                            throw new IOException("unexpected code " + response);
                        }
                        else
                        {
                            String responseBody = response.body().string();
                            Log.i(TAG, "server responded: " + responseBody);
                            try
                            {
                                ServerResultXmlParser.ServerResult result =
                                        new ServerResultXmlParser().parse(responseBody);
                                if (result.isSuccess())
                                {
                                    Toast.makeText(OrderActivity.this,
                                            "Success!", Toast.LENGTH_LONG).show();
                                }
                                else
                                {
                                    Toast.makeText(OrderActivity.this,
                                            "Purchase Failed",
                                            Toast.LENGTH_LONG).show();
                                }
                            } catch (XmlPullParserException e)
                            {
                                Log.e(TAG, "failed to parse xml");
                                Log.e(TAG, Log.getStackTraceString(e));
                            }
                            OrderActivity.this.finish();
                        }
                    }
                }
        );

    }
}
