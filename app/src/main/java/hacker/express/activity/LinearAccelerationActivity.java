package hacker.express.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import hacker.express.R;
import hacker.express.sensor.AccelerationSensor;
import hacker.express.sensor.LinearAccelerationSensor;
import hacker.express.sensor.observer.AccelerationSensorObserver;
import hacker.express.sensor.observer.LinearAccelerationSensorObserver;

public class LinearAccelerationActivity extends Activity implements Runnable, OnTouchListener,
        AccelerationSensorObserver, LinearAccelerationSensorObserver { //, GravitySensorObserver {
    //@SuppressLint("SimpleDateFormat")
    //private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMdd HH:mm:ss");

    private float[] acceleration = new float[3];
    private float[] linearAcceleration = new float[3];
    private float[] gyroscopeOrientation = new float[3];
    //private float[] gravity = new float[3];

    private AccelerationSensor accelerationSensor;
    private LinearAccelerationSensor linearAccelerationSensor;
    //private GravitySensor gravitySensor;

    private StringBuilder logBuilder;
    private Handler handler;

    private static File logFile;
    private static FileOutputStream FOS;

    private static PowerManager.WakeLock mWakeLock;

    private final int WRITE_EXTERNAL_STORAGE_PERMISSION_FLAG = 1;

    private static final int WRITE_THRESHOLD = 1500;

    private volatile int OBSERVED_COUNT = 0;

    private synchronized void collectCount() {
        OBSERVED_COUNT++;
    }

    private synchronized void collectCountReset() {
        OBSERVED_COUNT = 0;
    }

    private volatile boolean mainThreadExist = false;

    private static final int FRAME_RATE = 100;
    private static final float INTEGRAL_TIME_CONSTANT = (((float) FRAME_RATE) / 1000) * (((float) FRAME_RATE) / 1000);

    private float calibrateAccX = 0;
    private float calibrateAccY = 0;
    private float calibrateAccZ = 0;
    private float calibrateDistanceX = 0;
    private float calibrateDistanceY = 0;
    private float calibrateDistanceZ = 0;

    private static final String LOG_DIR = Environment.getExternalStorageDirectory()
            + File.separator + ".night" + File.separator + "elf";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_night_elf);
        acquireWakeLock(this);
        askPermissions();
    }

    protected void askPermissions() {
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS
                    },
                    WRITE_EXTERNAL_STORAGE_PERMISSION_FLAG);
        } else {
            moveTaskToBack(true);
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_STORAGE_PERMISSION_FLAG) {
            boolean has = ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            if (!has) {
                askPermissions();
            } else {
                moveTaskToBack(true);
            }
        }
    }


    private class LinearAccCalibration implements Runnable {
        public static final int DELAY = 15000;

        @Override
        public void run() {
            try {
                Thread.sleep(DELAY);
                calibrateAccX = -linearAcceleration[0];
                calibrateAccY = -linearAcceleration[1];
                calibrateAccZ = -linearAcceleration[2];
                calibrateDistanceX = -((linearAcceleration[0] + calibrateAccX) * INTEGRAL_TIME_CONSTANT);
                calibrateDistanceY = -((linearAcceleration[1] + calibrateAccY) * INTEGRAL_TIME_CONSTANT);
                calibrateDistanceZ = -((linearAcceleration[2] + calibrateAccZ) * INTEGRAL_TIME_CONSTANT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private volatile boolean hasCalibrate = false;

    private void startSensor() {
        linearAccelerationSensor = new LinearAccelerationSensor(this);
        accelerationSensor = new AccelerationSensor(this);
        //gravitySensor = new GravitySensor(this);

        handler = new Handler();
        handler.post(this);

        //gravitySensor.registerGravityObserver(this);

        accelerationSensor.registerAccelerationObserver(this);
        accelerationSensor.registerAccelerationObserver(linearAccelerationSensor);

        linearAccelerationSensor.registerAccelerationObserver(this);
        linearAccelerationSensor.onStart();
    }

    // start to calibrate offset
    private void startCalibrate() {
        if (!hasCalibrate) {
            try {
                Toast.makeText(this, "Let phone stand horizontally for 10 seconds...",
                        Toast.LENGTH_LONG).show();
                Thread calibrateThread = new Thread(new LinearAccCalibration());
                calibrateThread.start();
            } finally {
                hasCalibrate = true;
            }
        }
    }

    private void lucky() {
        if (mainThreadExist) {
            return;
        }
        mainThreadExist = true;
        initLogHeader();
        startCalibrate();
        startSensor();
        Toast.makeText(this, "elf is observing.", Toast.LENGTH_SHORT).show();
    }

    private void initLogHeader() {
        // logBuilder = new StringBuilder("T, AX, AY, AZ, lAX, lAY, lAZ, PITCH, ROLL, YAW, IN_PITCH, IN_ROLL, IN_AZIMUTH");
        // logBuilder = new StringBuilder("T, AX, AY, AZ, lAX, lAY, lAZ, GX,GY,GZ, PITCH, ROLL, AZIMUTH");
        logBuilder = new StringBuilder("T, lAX, lAY, lAZ, PITCH, ROLL, AZIMUTH, MX, MY, MZ");
    }


    @Override
    public void onAccelerationSensorChanged(float[] acceleration, long timeStamp) {
        System.arraycopy(acceleration, 0, this.acceleration, 0, acceleration.length);
    }

    @Override
    public void onLinearAccelerationSensorChanged(float[] linearAcceleration, float[] gyroscopeOrientation, long timeStamp) {
        System.arraycopy(linearAcceleration, 0, this.linearAcceleration, 0, linearAcceleration.length);
        System.arraycopy(gyroscopeOrientation, 0, this.gyroscopeOrientation, 0, gyroscopeOrientation.length);
    }

//    @Override
//    public void onGravitySensorChanged(float[] gravity, long timeStamp) {
//        System.arraycopy(gravity, 0, this.gravity, 0, gravity.length);
//    }

    @Override
    public void run() {
        handler.postDelayed(this, FRAME_RATE);
        logData();
    }

    /**
     * Log output data to an external .csv file.
     */
    private float la = 0, ly = 0, lz = 0;

    private void logData() {
        try {
            dumpAction();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        logBuilder.append(System.getProperty("line.separator"));
        logBuilder.append(System.currentTimeMillis()).append(",");

        //logBuilder.append(acceleration[0]).append(",");
        //logBuilder.append(acceleration[1]).append(",");
        //logBuilder.append(acceleration[2]).append(",");
        la = linearAcceleration[0] + calibrateAccX;
        ly = linearAcceleration[1] + calibrateAccY;
        lz = linearAcceleration[2] + calibrateAccZ;
        logBuilder.append(la).append(",");
        logBuilder.append(ly).append(",");
        logBuilder.append(lz).append(",");

        //logBuilder.append(gravity[0]).append(",");
        //logBuilder.append(gravity[1]).append(",");
        //logBuilder.append(gravity[2]).append(",");
        //// 转换欧拉角
        //EulerAngles eulerAngles = IMU.update(acceleration[0], acceleration[1], acceleration[2],
        //        linearAcceleration[0], linearAcceleration[1], linearAcceleration[2]);
        //logBuilder.append(eulerAngles.getPitch()).append(",");
        //logBuilder.append(eulerAngles.getRoll()).append(",");
        //logBuilder.append(eulerAngles.getYaw()).append(",");
        //// 机内自带姿态
        logBuilder.append(gyroscopeOrientation[1]).append(",");
        logBuilder.append(gyroscopeOrientation[2]).append(",");
        logBuilder.append(gyroscopeOrientation[0]).append(",");

        logBuilder.append(la * INTEGRAL_TIME_CONSTANT + calibrateDistanceX).append(",");
        logBuilder.append(ly * INTEGRAL_TIME_CONSTANT + calibrateDistanceY).append(",");
        logBuilder.append(lz * INTEGRAL_TIME_CONSTANT + calibrateDistanceZ).append(",");
        collectCount();
    }

    private void dumpAction() throws FileNotFoundException {
        if (OBSERVED_COUNT >= WRITE_THRESHOLD) {
            try {
                dumpData(logBuilder);
            } finally {
                collectCountReset();
            }
        }
    }

    private void dumpData(StringBuilder stringBuilder) throws FileNotFoundException {
        File dir = new File(LOG_DIR);
        if (!dir.exists()) {
            boolean result = dir.mkdirs();
            System.out.println("PATH NOT EXIST, CREATE ACTION RESULT: " + result);
        }
        if (logFile == null) {
            String LOG_FILE_NAME = "elf.csv";
            logFile = new File(dir, LOG_FILE_NAME);
        }
        if (FOS == null) {
            FOS = new FileOutputStream(logFile, true);
        }
        try {
            byte[] data = logBuilder.toString().getBytes();
            FOS.write(data);
            FOS.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            stringBuilder.setLength(0);
        }
    }

    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }

    @Override
    public void onPause() {
        super.onPause();
        //        accelerationSensor.removeAccelerationObserver(this);
        //        accelerationSensor.removeAccelerationObserver(linearAccelerationSensor);
        //        linearAccelerationSensor.removeAccelerationObserver(this);
        //        linearAccelerationSensor.onPause();
        //        if (logData) {
        //            writeLogToFile();
        //        }
        //        handler.removeCallbacks(this);

        System.err.println("on pause");
    }

    //申请设备电源锁
    @SuppressLint("InvalidWakeLockTag")
    public static void acquireWakeLock(Context context) {
        if (null == mWakeLock) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            mWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ON_AFTER_RELEASE, "WakeLock");
            if (null != mWakeLock) {
                mWakeLock.acquire();
            }
        }
    }

    //释放设备电源锁
    public static void releaseWakeLock() {
        if (null != mWakeLock) {
            mWakeLock.release();
            mWakeLock = null;
        }
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        moveTaskToBack(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mainThreadExist) return;
        lucky();
    }

    @Override
    public void onDestroy() {
        releaseWakeLock();
        mainThreadExist = false;
        lucky();
        super.onDestroy();
    }

}
