package com.greenpay.androidapp;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

//this activity shows up after the camera detects a barcode of greenpay
public class OrderActivity extends AppCompatActivity
{
    //a text view that shows the details of the store (name, city, phoneno, etc)
    TextView orderDetails;

    //a textview that shows the receipt of the order
    TextView orderContent;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order);
        //initalize the ui elements
        orderDetails = findViewById(R.id.order_details);
        orderContent = findViewById(R.id.order_content);
        //get the details from the activity's intent
        Intent intent = getIntent();
        int orderId = intent.getIntExtra("orderId", 0);
        int storeId = intent.getIntExtra("storeId", 0);
        String storeName = intent.getStringExtra("storeName");
        String storeCity = intent.getStringExtra("storeCity");
        String storeEmail = intent.getStringExtra("storeEmail");
        int price = intent.getIntExtra("price", 0);
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
}
