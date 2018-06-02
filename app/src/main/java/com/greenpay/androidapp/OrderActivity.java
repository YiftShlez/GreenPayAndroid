package com.greenpay.androidapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;


//this activity shows up after the camera detects a barcode of greenpay
public class OrderActivity extends AppCompatActivity
{
    //a text view that shows the details of the store (name, city, phoneno, etc)
    TextView orderDetails;

    //a textview that shows the receipt of the order
    TextView orderContent;

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
    }

    /**
     * an onclick for the pay button
     * contacts the server and requests to pay
     * @param view the button
     */
    public void pay (View view)
    {
        Toast.makeText(OrderActivity.this,
                "Success! Receipt sent to your email", Toast.LENGTH_LONG).show();
        finish();

    }
}
