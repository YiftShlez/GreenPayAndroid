package com.greenpay.androidapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.vision.CameraSource;
import com.google.android.gms.vision.Detector;
import com.google.android.gms.vision.barcode.Barcode;
import com.google.android.gms.vision.barcode.BarcodeDetector;

import java.io.IOException;
import java.lang.reflect.Field;
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
                                url(HttpUrl.parse("http://greenpayserver.tk")
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
                                        }
                                        else
                                        {

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
            //for some reason, the camera work only after I stop it
            cameraSource.stop();
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults)
    {
        if (requestCode == camera_request)
        {
            if (grantResults.length > 0)
            {
                if (checkCameraPermittion(this))
                {
                    cameraSource.stop();
                    startCamera();
                } else
                {
                    mainText1.setText(R.string.camera_access_msg);
                    requestAccessBtn.setVisibility(View.VISIBLE);
                }
            }
        } else
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }


    public void requestCameraAccess(View view)
    {
        requestCameraPermission(this);
    }

    @Override
    protected void onPause()
    {
        cameraSource.stop();
        super.onPause();
    }

    @Override
    protected void onRestart()
    {
        if (checkCameraPermittion(this))
            startCamera();
        super.onRestart();
    }
}
