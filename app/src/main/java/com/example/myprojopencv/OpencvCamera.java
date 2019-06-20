package com.example.myprojopencv;

import android.content.Context;
import android.graphics.Bitmap;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseArray;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

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

public class OpencvCamera extends AppCompatActivity implements CameraBridgeViewBase.CvCameraViewListener2, SensorEventListener {

    private static final String TAG = "OpencvCamera";
    private CameraBridgeViewBase cameraBridgeViewBase;
    private int maxFaceCount = 0;
    private float mOrientation = 0f;

    private SensorManager mSensorManager;
    private Sensor mLight;

    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];
    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];


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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_opencv_camera);
        cameraBridgeViewBase = (CameraBridgeViewBase) findViewById(R.id.camera_view);
        cameraBridgeViewBase.setVisibility(SurfaceView.VISIBLE);
        cameraBridgeViewBase.setCvCameraViewListener(this);

        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        mLight = mSensorManager.getDefaultSensor(Sensor.TYPE_LIGHT);
    }

    @Override
    public void onResume(){
        super.onResume();

        // Position Sensor
        mSensorManager.registerListener(this, mLight, SensorManager.SENSOR_DELAY_NORMAL);

        Sensor accelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            mSensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }

        Sensor magneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            mSensorManager.registerListener(this, magneticField, SensorManager.SENSOR_DELAY_NORMAL, SensorManager.SENSOR_DELAY_UI);
        }

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
            tv1.setText("People: " + Integer.toString(faceCount )+ "  Peak: " + Integer.toString(maxFaceCount) + " O1: " +  rotationMatrix[0] + " O2: " + rotationMatrix[1] + " O3: " + rotationMatrix[2]);

            TextView tv2 = (TextView) findViewById(R.id.loc_data);
            tv2.setText("Test");


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

    @Override
    public void onSensorChanged(SensorEvent event) {

        updateOrientationAngles();

        float[] mGravity = null;
        float[] mGeomagnetic = null;

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER)
            mGravity = event.values;

        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD)
            mGeomagnetic = event.values;

        if (mGravity != null && mGeomagnetic != null) {

            float R[] = new float[9];
            float I[] = new float[9];

            if (SensorManager.getRotationMatrix(R, I, mGravity, mGeomagnetic)) {

                // orientation contains azimut, pitch and roll
                float orientation[] = new float[3];
                SensorManager.getOrientation(R, orientation);

                mOrientation = 3;

                mOrientation = orientation[0] * 360 / (2 * 3.14159f);
            }
        }

        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading,0, accelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading, 0, magnetometerReading.length);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    public void updateOrientationAngles() {

        // Update rotation matrix, which is needed to update orientation angles.
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading);
        SensorManager.getOrientation(rotationMatrix, orientationAngles);
    }


}