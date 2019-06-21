package com.example.myprojopencv;

import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Location;
import android.os.Build;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceView;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.vision.Frame;
import com.google.android.gms.vision.face.Face;
import com.google.android.gms.vision.face.FaceDetector;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class OpencvCamera extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, LocationListener {

    private static final String TAG = "OpencvCamera";
    private CameraBridgeViewBase cameraBridgeViewBase;
    private int maxFaceCount = 0;

    private GoogleMap mMap;
    private GoogleApiClient mGoogleApiClient;
    private LocationRequest mLocationRequest;

    private float range = 0.001f;
    private Location mLastLocation;
    private Location mLocation;

    private float lat;
    private float lon;

    private Location loc_1 = new Location("");
    private Location loc_2 = new Location("");
    private Location loc_3 = new Location("");
    private Location loc_4 = new Location("");

    private String exits = "Exits";

    private BaseLoaderCallback baseLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    cameraBridgeViewBase.enableView();
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };

    public boolean checkLocationPermission(){

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)  != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this, android.Manifest.permission.ACCESS_COARSE_LOCATION)) {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            } else {
                requestPermissions(new String[]{android.Manifest.permission.ACCESS_COARSE_LOCATION}, 1);
            }

            return false;

        } else {
            return true;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_opencv_camera);
        cameraBridgeViewBase = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);

        Button addInit = (Button) findViewById(R.id.add_record);
        addInit.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { addLoc(); }
        });

        Button resetInit = (Button) findViewById(R.id.reset);
        resetInit .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                maxFaceCount = 0;
            }
        });

        Button mapInit = (Button) findViewById(R.id.show_map);
        mapInit .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent i = new Intent(getApplicationContext(),MapsActivity.class);
                startActivity(i);
            }
        });

        Button clearInit = (Button) findViewById(R.id.clear);
        clearInit  .setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { clear(); }
        });

        TextView tv2 = (TextView) findViewById(R.id.loc_data);
        tv2.setText(exits);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission();
        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map2);
        mapFragment.getMapAsync(this);


        if (mLastLocation != null) {
            lat = (float) mLastLocation.getLatitude();
            lon = (float) mLastLocation.getLongitude();

            loc_1.setLatitude(lat + range);
            loc_1.setLatitude(lon);
            loc_2.setLatitude(lat);
            loc_2.setLongitude(lon + range);
            loc_3.setLatitude(lat - range);
            loc_3.setLongitude(lon);
            loc_4.setLatitude(lat);
            loc_4.setLongitude(lon - range);
        }
    }

    @Override
    public void onResume(){
        super.onResume();

        // OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_1_0, this, baseLoaderCallback);
        } else {
            Log.d(TAG, "OpenCV library found inside package. Using it!");
            baseLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

    }

    @Override
    public void onCameraViewStarted(int width, int height) {
    }

    @Override
    public void onCameraViewStopped() {

    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        Mat mRgba = inputFrame.rgba();
        Mat mRGBAt = mRgba.t();

        int width = mRGBAt.cols();
        int height = mRGBAt.rows();

        FaceDetector faceDetector = new FaceDetector.Builder(getApplicationContext())
                .setTrackingEnabled(false)
                .setMode(FaceDetector.FAST_MODE)
                .build();

        if (!faceDetector.isOperational())
        {
            Toast.makeText(OpencvCamera.this, "Face Detector could not be set up on your device", Toast.LENGTH_SHORT).show();
        }
        else {

            final Bitmap myBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
            Utils.matToBitmap(mRGBAt, myBitmap);

            Frame frame = new Frame.Builder().setBitmap(myBitmap).build();
            SparseArray<Face> sparseArray = faceDetector.detect(frame);

            // Show number of people
            final int faceCount = sparseArray.size();

            if (faceCount > maxFaceCount) {
                maxFaceCount = faceCount;
            }

            TextView tv1 = (TextView) findViewById(R.id.camera_data);
            tv1.setText("People: " + Integer.toString(faceCount )+ "  Peak: " + Integer.toString(maxFaceCount));

            for (int i = 0; i < sparseArray.size(); i++) {
                Face face = sparseArray.valueAt(i);

                float x1 = face.getPosition().y;
                float y1 = face.getPosition().x;
                float x2 = x1 + face.getWidth();
                float y2 = y1 + face.getHeight();

                Imgproc.rectangle(mRgba, new Point(x1, y1), new Point(x2, y2), new Scalar(0, 255, 0, 255), 3);
            }
        }

        return mRgba;
    }

    public void addLoc() {

        int loc = 1;

        if (mLastLocation != null && mLocation != null) {

            float minDistance = mLocation.distanceTo(loc_1);

            if (mLocation.distanceTo(loc_2) < minDistance) {
                minDistance = mLocation.distanceTo(loc_2);
                loc = 2;
            }

            if (mLocation.distanceTo(loc_3) < minDistance) {
                minDistance = mLocation.distanceTo(loc_3);
                loc = 3;
            }

            if (mLocation.distanceTo(loc_4) < minDistance) {
                minDistance = mLocation.distanceTo(loc_4);
                loc = 4;
            }
        }

        exits += "\nExit: " + Integer.toString(loc) + " People: " + maxFaceCount;

        maxFaceCount = 0;

        TextView tv2 = (TextView) findViewById(R.id.loc_data);
        tv2.setText(exits);
    }

    public void clear() {
        exits = "Exits";

        TextView tv2 = (TextView) findViewById(R.id.loc_data);
        tv2.setText(exits);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case 1: {

                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }
                }
                return;
            }
        }
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(true);
            }
        }
        else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

    }


    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onLocationChanged(Location location) {

        if (location != null)
        {
            mLocation = location;

            if (mLastLocation == null) {
                mLastLocation = location;

                lat = (float) mLastLocation.getLatitude();
                lon = (float) mLastLocation.getLongitude();

                loc_1.setLatitude(lat + range);
                loc_1.setLatitude(lon);
                loc_2.setLatitude(lat);
                loc_2.setLongitude(lon + range);
                loc_3.setLatitude(lat - range);
                loc_3.setLongitude(lon);
                loc_4.setLatitude(lat);
                loc_4.setLongitude(lon - range);
            }
        }

    }

    @Override
    public void onConnected(Bundle bundle) {
        TextView tv2 = (TextView) findViewById(R.id.loc_data);
        tv2.setText(exits);

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(2000);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

}