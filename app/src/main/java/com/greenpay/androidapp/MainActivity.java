package com.greenpay.androidapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * The main activity of the app
 * The activity scans qr code and sends it to the server
 *
 * @author Yiftah Schlesinger
 * @version 1.0
 */
public class MainActivity extends AppCompatActivity
{

    static final String TAG = "com.greenpay.androidapp"; //log tag

    BarcodeDetector barcodeDetector; //for detecting qr codes
    CameraSource cameraSource; //for getting the images from the camera

    //an id for the camera permission request
    static final int camera_request = 1823;

    TextView mainText1;
    Button requestAccessBtn;

    //an HTTP client to handle communication with server
    OkHttpClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mainText1 = findViewById(R.id.main_text1);
        requestAccessBtn = findViewById(R.id.main_access_btn);
        client = new OkHttpClient();
        //set the barcode detector to detect QR codes
        barcodeDetector = new BarcodeDetector.Builder(this).
                setBarcodeFormats(Barcode.QR_CODE).
                build();
        cameraSource = new CameraSource.Builder(this, barcodeDetector).
                setRequestedPreviewSize(640, 480).
                build();
        //set the listener for barcode detections
        barcodeDetector.setProcessor(new Detector.Processor<Barcode>()
        {
            String lastBarcode = "";

            @Override
            public void release()
            {

            }

            //this function is executed every time the app detects a barcode
            @Override
            public void receiveDetections(Detector.Detections<Barcode> detections)
            {
                //The array of the barcodes
                SparseArray<Barcode> barcodes = detections.getDetectedItems();
                if (barcodes.size() > 0)
                {
                    //The value of the barcode
                    String barcodeValue = barcodes.valueAt(0).displayValue;
                    if (barcodeValue.equals(lastBarcode))
                        return;
                    else
                        lastBarcode = barcodeValue;
                    Log.i(TAG, "Found barcode: " + barcodeValue);
                    //check if the barcode belongs to GreenPay
                    if (barcodeValue.startsWith("{greenpaycode}:"))
                    {
                        //the unique code of the barcode
                        String code = barcodeValue.split(":")[1];
                        //a http request to get barcode info
                        Request request = new Request.Builder().
                                url(HttpUrl.parse("http://10.0.0.13:34526/GreenPay")
                                        .newBuilder()
                                        .addQueryParameter("request", "checkbarcode")
                                        .addQueryParameter("barcode", code)
                                        .build().toString()).build();
                        //send the request
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
                                            throw new IOException("Unexpected code " + response);
                                        } else
                                        {
                                            String responseBody = response.body().string();
                                            Log.i(TAG, "server responded: " + responseBody);
                                            try
                                            {
                                                ServerResultXmlParser.ServerResult result =
                                                        new ServerResultXmlParser().parse(responseBody);
                                                if (result.isSuccess())
                                                {
                                                    List children = result.getChildren();
                                                    if (children.size() > 0)
                                                    {
                                                        ServerResultXmlParser.Order order =
                                                                (ServerResultXmlParser.Order)
                                                                        children.get(0);
                                                        Log.i(TAG, "content: " + order.getContent());
                                                        Intent intent = new Intent(
                                                                MainActivity.this, OrderActivity.class
                                                        );
                                                        int orderId = order.getOrderId();
                                                        int storeId = order.getStoreId();
                                                        String storeName = order.getStoreName();
                                                        String storeCity = order.getStoreCity();
                                                        String storeEmail = order.getStoreEmail();
                                                        int price = order.getPrice();
                                                        String content = order.getContent();
                                                        intent.putExtra("orderId", orderId);
                                                        intent.putExtra("storeId", storeId);
                                                        intent.putExtra("storeName", storeName);
                                                        intent.putExtra("storeCity", storeCity);
                                                        intent.putExtra("storeEmail", storeEmail);
                                                        intent.putExtra("price", price);
                                                        intent.putExtra("content", content);
                                                        startActivity(intent);
                                                    }
                                                }
                                            } catch (XmlPullParserException | IOException e)
                                            {
                                                Log.e(TAG, Log.getStackTraceString(e));
                                            }
                                        }
                                    }
                                }
                        );
                    }
                }
            }
        });
        //check if the application has permission to camera
        if (checkCameraPermittion(MainActivity.this))
        {
            //start the camera
            startCamera();
            //focus the camera
            cameraFocus(cameraSource, Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } else
            //request permission to camera
            requestCameraPermission(MainActivity.this);
    }

    /**
     * This function is used to focus the camera.
     *
     * @param cameraSource The camera source that will be focused
     * @param focusMode    the focus mode
     * @return true if the focus succeded
     */
    private static boolean cameraFocus(@NonNull CameraSource cameraSource, @NonNull String focusMode)
    {
        Field[] declaredFields = CameraSource.class.getDeclaredFields();

        for (Field field : declaredFields)
        {
            if (field.getType() == Camera.class)
            {
                field.setAccessible(true);
                try
                {
                    Camera camera = (Camera) field.get(cameraSource);
                    if (camera != null)
                    {
                        Camera.Parameters params = camera.getParameters();
                        params.setFocusMode(focusMode);
                        camera.setParameters(params);
                        return true;
                    }

                    return false;
                } catch (IllegalAccessException e)
                {
                    e.printStackTrace();
                }

                break;
            }
        }

        return false;
    }

    /**
     * this function starts scanning the qr code from the camera
     */
    private void startCamera()
    {
        //change the text on the textview
        mainText1.setText(R.string.point_camera);
        try
        {
            cameraSource.stop();
            //start getting images from camera
            cameraSource.start();
        } catch (IOException | SecurityException e)
        {
            Log.e(TAG, Log.getStackTraceString(e));
            mainText1.setText(R.string.error);
            return;
        }
    }

    /**
     * This function checks if the app has permission to camera
     *
     * @param context the activity
     * @return true if the app has access, false otherwise
     */
    public static boolean checkCameraPermittion(Context context)
    {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * This function requests a permission to camera
     *
     * @param activity the activity which the function is called from
     */
    public static void requestCameraPermission(Activity activity)
    {
        ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA}, camera_request);
    }

    @Override
    //this function is called when the user grants or rejects a permission
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        //check if the permission is a permission to use the camera
        if (requestCode == camera_request)
        {
            if (grantResults.length > 0)
            {
                //check if the user granted the permission
                if (checkCameraPermittion(this))
                {
                    startCamera();
                } else
                {
                    //show a message that explains why the app needs camera permission
                    mainText1.setText(R.string.camera_access_msg);
                    requestAccessBtn.setVisibility(View.VISIBLE);
                }
            }
        } else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    //onclick for the "request permission" button
    public void requestCameraAccess(View view)
    {
        requestCameraPermission(this);
    }

    @Override
    //when the user leaves the app, stop the camera service
    protected void onPause()
    {
        cameraSource.stop();
        super.onPause();
    }

    @Override
    //when the user returns to the app, restart the camera service
    protected void onRestart()
    {
        if (checkCameraPermittion(this))
            startCamera();
        super.onRestart();
    }
}

//a XML parser for the server result
class ServerResultXmlParser
{
    //a function to get a ServerResult from XML string
    ServerResult parse(String xml) throws XmlPullParserException, IOException
    {
        //InputStream that reads the xml string
        InputStream in = new ByteArrayInputStream(
                xml.getBytes(StandardCharsets.UTF_8));
        try
        {
            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(in, null);
            parser.nextTag();
            return readResult(parser);
        } finally
        {
            in.close();
        }
    }

    //this function generates a ServerResult object from a <result> tag
    private ServerResult readResult(XmlPullParser parser) throws XmlPullParserException, IOException
    {
        parser.require(XmlPullParser.START_TAG, null, "result");
        boolean success = parser.getAttributeValue(null, "success").equals("true");
        int error = success ? 0 : Integer.parseInt(parser.getAttributeValue(null, "error"));
        ServerResult result = new ServerResult(success, error);
        while (parser.next() != XmlPullParser.END_TAG)
        {
            if (parser.getEventType() != XmlPullParser.START_TAG)
                continue;
            String name = parser.getName();
            if (name.equals("order"))
            {
                result.addChild(readOrder(parser));
            } else
            {
                skip(parser);
            }
        }
        return result;
    }

    //this function generates an Order object from an <order> tag
    private Order readOrder(XmlPullParser parser) throws XmlPullParserException, IOException
    {
        parser.require(XmlPullParser.START_TAG, null, "order");
        int orderId = Integer.parseInt(parser.getAttributeValue(null, "orderId"));
        int storeId = Integer.parseInt(parser.getAttributeValue(null, "orderId"));
        String storeName = parser.getAttributeValue(null, "storeName");
        String storeCity = parser.getAttributeValue(null, "storeCity");
        String storeEmail = parser.getAttributeValue(null, "storeEmail");
        int price = Integer.parseInt(parser.getAttributeValue(null, "price"));
        parser.next();
        String content = parser.getText();
        return new Order(orderId, storeId, storeName, storeCity, storeEmail, price, content);
    }

    //this function skips a tag that the app doesn't care about
    private void skip(XmlPullParser parser) throws XmlPullParserException, IOException
    {
        if (parser.getEventType() != XmlPullParser.START_TAG)
            throw new IllegalStateException();
        int depth = 1;
        while (depth != 0)
        {
            switch (parser.next())
            {
                case XmlPullParser.END_TAG:
                    depth--;
                    break;
                case XmlPullParser.START_TAG:
                    depth++;
                    break;
            }
        }
    }

    //a data class used to store an <order> tag and its attributes
    public class Order
    {
        private int orderId;
        private int storeId;
        private String storeName;
        private String storeCity;
        private String storeEmail;
        private int price;
        private String content;

        public Order(int orderId, int storeId, String storeName, String storeCity, String storeEmail, int price, String content)
        {
            this.orderId = orderId;
            this.storeId = storeId;
            this.storeName = storeName;
            this.storeCity = storeCity;
            this.storeEmail = storeEmail;
            this.price = price;
            this.content = content;
        }

        public String getContent()
        {
            return content;
        }

        public int getOrderId()
        {
            return orderId;
        }

        public int getStoreId()
        {
            return storeId;
        }

        public String getStoreName()
        {
            return storeName;
        }

        public String getStoreCity()
        {
            return storeCity;
        }

        public String getStoreEmail()
        {
            return storeEmail;
        }

        public int getPrice()
        {
            return price;
        }
    }

    //a data class used to store a <result> tag
    public class ServerResult
    {
        private boolean success;
        private int error;
        private List children;

        public ServerResult(boolean success, int error)
        {
            this.success = success;
            this.error = error;
            children = new ArrayList();
        }

        public boolean isSuccess()
        {
            return success;
        }

        public int getError()
        {
            return error;
        }

        public List getChildren()
        {
            return children;
        }

        public void addChild(Object child)
        {
            children.add(child);
        }
    }
}