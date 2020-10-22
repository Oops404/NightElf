package hacker.express.service;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Intent;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import hacker.express.sensor.AccelerationSensor;
import hacker.express.sensor.GyroscopeSensor;
import hacker.express.sensor.LinearAccelerationOfficialSensor;
import hacker.express.sensor.observer.AccelerationSensorObserver;
import hacker.express.sensor.observer.GyroscopeSensorObserver;
import hacker.express.sensor.observer.LinearAccelerationOfficialObserver;

public class ElfService extends Service implements Runnable, OnTouchListener,
        AccelerationSensorObserver, LinearAccelerationOfficialObserver, GyroscopeSensorObserver {
    //, GravitySensorObserver {

    private float[] acceleration = new float[3];
    //private float[] linearAcceleration = new float[3];
    private float[] linearAccelerationOfficial = new float[3];
    //private float[] gyroscopeOrientation = new float[3];
    private float[] gyroscopeOrientationOfficial = new float[3];
    //private float[] gravity = new float[3];

    private AccelerationSensor accelerationSensor;
    //private LinearAccelerationSensor linearAccelerationSensor;
    private LinearAccelerationOfficialSensor linearAccelerationOfficialSensor;
    //private GravitySensor gravitySensor;
    private GyroscopeSensor gyroscopeSensor;
    private StringBuilder logBuilder;
    private Handler handler;

    private static File logFile;
    private static FileOutputStream FOS;

    private static final int WRITE_THRESHOLD = 3000;

    private volatile int OBSERVED_COUNT = 0;

    private synchronized void collectCount() {
        OBSERVED_COUNT++;
    }

    private synchronized void collectCountReset() {
        OBSERVED_COUNT = 0;
    }

    private volatile boolean mainThreadExist = false;

    private static final int SECOND = 1000;
    private static final int FRAME_RATE = SECOND / 20;
    private static final float T = ((float) FRAME_RATE) / SECOND;
    private static final double T2 = Math.pow(T, 2);
    private static final float INTEGRAL_TIME_CONSTANT = T * T;

    private float calibrateAccX = 0;
    private float calibrateAccY = 0;
    private float calibrateAccZ = 0;
    private float calibrateLinearAccOfficialX = 0;
    private float calibrateLinearAccOfficialY = 0;
    private float calibrateLinearAccOfficialZ = 0;

    private float calibrateDistanceX = 0;
    private float calibrateDistanceY = 0;
    private float calibrateDistanceZ = 0;

    private static final String LOG_DIR = Environment.getExternalStorageDirectory()
            + File.separator + ".night" + File.separator + "elf";


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

    @Override
    public void onCreate() {
        super.onCreate();
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
        public static final int DELAY = 10000;

        @Override
        public void run() {
            try {
                Thread.sleep(DELAY);
                //calibrateAccX = -linearAcceleration[0];
                //calibrateAccY = -linearAcceleration[1];
                //calibrateAccZ = -linearAcceleration[2];
                calibrateLinearAccOfficialX = -linearAccelerationOfficial[0];
                calibrateLinearAccOfficialY = -linearAccelerationOfficial[1];
                calibrateLinearAccOfficialZ = -linearAccelerationOfficial[2];
                // calibrateDistanceX = -((linearAcceleration[0] + calibrateAccX) * INTEGRAL_TIME_CONSTANT);
                // calibrateDistanceY = -((linearAcceleration[1] + calibrateAccY) * INTEGRAL_TIME_CONSTANT);
                // calibrateDistanceZ = -((linearAcceleration[2] + calibrateAccZ) * INTEGRAL_TIME_CONSTANT);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }

    private volatile boolean hasCalibrate = false;

    private void startSensor() {
        // linearAccelerationSensor = new LinearAccelerationSensor(this);
        accelerationSensor = new AccelerationSensor(this);
        linearAccelerationOfficialSensor = new LinearAccelerationOfficialSensor(this);
        //gravitySensor = new GravitySensor(this);
        gyroscopeSensor = new GyroscopeSensor(this);

        handler = new Handler();
        handler.post(this);

        gyroscopeSensor.registerGyroscopeObserver(this);
        //gravitySensor.registerGravityObserver(this);
        linearAccelerationOfficialSensor.registerLinearAccelerationObserver(this);

        accelerationSensor.registerAccelerationObserver(this);

        //accelerationSensor.registerAccelerationObserver(linearAccelerationSensor);
        //linearAccelerationSensor.registerAccelerationObserver(this);
        //linearAccelerationSensor.onStart();

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
        logBuilder = new StringBuilder("T, AX, AY, AZ, LAX, LAY, LAZ, PITCH, ROLL, AZIMUTH, MX, MY, MZ");
    }

    @Override
    public void run() {
        handler.postDelayed(this, FRAME_RATE);
        logData();
    }

    /**
     * Log output data to an external .csv file.
     */
    private float ax = 0, ay = 0, az = 0;
    private float lax = 0, lay = 0, laz = 0;
    private float vx = 0, vy = 0, vz = 0;

    private void logData() {
        try {
            dumpAction();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        logBuilder.append(System.getProperty("line.separator"));
        logBuilder.append(System.currentTimeMillis()).append(",");

        ax = acceleration[0] + calibrateLinearAccOfficialX;
        ay = acceleration[1] + calibrateLinearAccOfficialY;
        az = acceleration[2] + calibrateLinearAccOfficialZ;

        logBuilder.append(ax).append(",");
        logBuilder.append(ay).append(",");
        logBuilder.append(az).append(",");

        lax = linearAccelerationOfficial[0] + calibrateLinearAccOfficialX;
        lay = linearAccelerationOfficial[1] + calibrateLinearAccOfficialY;
        laz = linearAccelerationOfficial[2] + calibrateLinearAccOfficialZ;

        logBuilder.append(lax).append(",");
        logBuilder.append(lay).append(",");
        logBuilder.append(laz).append(",");

        logBuilder.append(gyroscopeOrientationOfficial[0]).append(",");
        logBuilder.append(gyroscopeOrientationOfficial[1]).append(",");
        logBuilder.append(gyroscopeOrientationOfficial[2]).append(",");

        vx += ((int) lax) * T;
        vy += ((int) lay) * T;
        vz += ((int) laz) * T;
        logBuilder.append(vx * T + 0.5 * lax * T2).append(",");
        logBuilder.append(vy * T + 0.5 * lay * T2).append(",");
        logBuilder.append(vz * T + 0.5 * laz * T2).append(",");
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

    //@Override
    //public void onLinearAccelerationSensorChanged(float[] linearAcceleration, float[] gyroscopeOrientation, long timeStamp) {
    //    System.arraycopy(linearAcceleration, 0, this.linearAcceleration, 0, linearAcceleration.length);
    //    System.arraycopy(gyroscopeOrientation, 0, this.gyroscopeOrientation, 0, gyroscopeOrientation.length);
    //}

    @Override
    public void onLinearAccelerationOfficialSensorChanged(float[] linearAcceleration, long timeStamp) {
        System.arraycopy(linearAcceleration, 0, this.linearAccelerationOfficial, 0, linearAcceleration.length);
    }
    //    @Override
    //    public void onGravitySensorChanged(float[] gravity, long timeStamp) {
    //        System.arraycopy(gravity, 0, this.gravity, 0, gravity.length);
    //    }

    @Override
    public void onGyroscopeSensorChanged(float[] gyroscope, long timeStamp) {
        System.arraycopy(gyroscope, 0, this.gyroscopeOrientationOfficial, 0, gyroscope.length);
    }

    @Override
    public void onDestroy() {
        mainThreadExist = false;
        lucky();
        Intent restartIntent = new Intent();
        restartIntent.setClass(this, ElfService.class);
        this.startService(restartIntent);
    }

}
