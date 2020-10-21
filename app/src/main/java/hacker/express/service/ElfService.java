package hacker.express.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import hacker.express.sensor.AccelerationSensor;
import hacker.express.sensor.LinearAccelerationSensor;
import hacker.express.sensor.observer.AccelerationSensorObserver;
import hacker.express.sensor.observer.LinearAccelerationSensorObserver;

public class ElfService extends Service implements Runnable, OnTouchListener,
        AccelerationSensorObserver, LinearAccelerationSensorObserver {
    //, GravitySensorObserver {

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

    private static PowerManager.WakeLock mWakeLock;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent motionEvent) {
        return false;
    }

    //申请设备电源锁
    @SuppressLint("InvalidWakeLockTag")
    public static void acquireWakeLock(Context context) {
        if (null == mWakeLock) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                mWakeLock = pm.newWakeLock(
                        PowerManager.PARTIAL_WAKE_LOCK
                                | PowerManager.ON_AFTER_RELEASE, "WakeLock"
                );
            }
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
    public void onCreate() {
        super.onCreate();
        acquireWakeLock(this);
        lucky();
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

    private void initLogHeader() {
        // logBuilder = new StringBuilder("T, AX, AY, AZ, lAX, lAY, lAZ, PITCH, ROLL, YAW, IN_PITCH, IN_ROLL, IN_AZIMUTH");
        // logBuilder = new StringBuilder("T, AX, AY, AZ, lAX, lAY, lAZ, GX,GY,GZ, PITCH, ROLL, AZIMUTH");
        logBuilder = new StringBuilder("T, lAX, lAY, lAZ, PITCH, ROLL, AZIMUTH, MX, MY, MZ");
    }

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
            byte[] data = stringBuilder.toString().getBytes();
            FOS.write(data);
            FOS.flush();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            stringBuilder.setLength(0);
        }
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
    public void onDestroy() {
        mainThreadExist = false;
        lucky();
        Intent restartIntent = new Intent();
        restartIntent.setClass(this, ElfService.class);
        this.startService(restartIntent);
    }

}
