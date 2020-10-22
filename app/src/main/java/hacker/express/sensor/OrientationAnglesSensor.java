package hacker.express.sensor;

import android.annotation.TargetApi;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;

import hacker.express.sensor.observer.OrientationAnglesSensorObserver;
import hacker.express.sensor.observer.RotationVectorSensorObserver;

public class OrientationAnglesSensor implements SensorEventListener {
    private SensorManager sensorManager;
    private final float[] accelerometerReading = new float[3];
    private final float[] magnetometerReading = new float[3];

    private final float[] rotationMatrix = new float[9];
    private final float[] orientationAngles = new float[3];

    private Context context;
    private long timeStamp = 0;
    private OrientationAnglesSensorObserver orientationAnglesSensorObserver;

    public OrientationAnglesSensor(Context context) {
        this.context = context;
        sensorManager = (SensorManager) this.context.getSystemService(Context.SENSOR_SERVICE);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    public void registerOrientationAnglesObserver(OrientationAnglesSensorObserver observer) {
        Sensor accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer,
                    SensorManager.SENSOR_DELAY_FASTEST, SensorManager.SENSOR_DELAY_UI);
        }
        Sensor magneticField = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        if (magneticField != null) {
            sensorManager.registerListener(this, magneticField,
                    SensorManager.SENSOR_DELAY_FASTEST, SensorManager.SENSOR_DELAY_UI);
        }
        this.orientationAnglesSensorObserver = observer;
    }

    public void removeOrientationAnglesObserver() {
        sensorManager.unregisterListener(this);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            System.arraycopy(event.values, 0, accelerometerReading,
                    0, accelerometerReading.length);
        } else if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            System.arraycopy(event.values, 0, magnetometerReading,
                    0, magnetometerReading.length);
        }
        updateOrientationAngles();
        timeStamp = event.timestamp;
        notifyOrientationAnglesObserver();
    }

    private void notifyOrientationAnglesObserver() {
        orientationAnglesSensorObserver.onOrientationAnglesSensorChanged(this.orientationAngles, this.timeStamp);
    }

    public void updateOrientationAngles() {
        SensorManager.getRotationMatrix(rotationMatrix, null, accelerometerReading, magnetometerReading);
        SensorManager.getOrientation(rotationMatrix, orientationAngles);
    }


    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

}
