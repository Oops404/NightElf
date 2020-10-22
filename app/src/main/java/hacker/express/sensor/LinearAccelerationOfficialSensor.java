package hacker.express.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.ArrayList;

import hacker.express.sensor.observer.LinearAccelerationOfficialObserver;

public class LinearAccelerationOfficialSensor implements SensorEventListener {
    private static final String tag = LinearAccelerationSensor.class.getSimpleName();

    private SensorManager sensorManager;
    private Sensor sensor;
    private Context context;
    private ArrayList<LinearAccelerationOfficialObserver> observersLinearAccelerationOfficial;

    public LinearAccelerationOfficialSensor(Context context) {
        this.context = context;
        this.sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
        observersLinearAccelerationOfficial = new ArrayList<LinearAccelerationOfficialObserver>();
    }

    public void registerLinearAccelerationObserver(LinearAccelerationOfficialObserver observer) {
        if (observersLinearAccelerationOfficial.size() == 0) {
            sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION),
                    SensorManager.SENSOR_DELAY_FASTEST
            );
        }
        int index = observersLinearAccelerationOfficial.indexOf(observer);
        if (index == -1) {
            observersLinearAccelerationOfficial.add(observer);
        }
    }

    public void removeLinearAccelerationObserver(LinearAccelerationOfficialObserver observer) {
        int index = observersLinearAccelerationOfficial.indexOf(observer);
        if (index >= 0) {
            observersLinearAccelerationOfficial.remove(index);
        }
        if (observersLinearAccelerationOfficial.size() == 0) {
            sensorManager.unregisterListener(this);
        }
    }

    private long timeStamp;
    private float[] linearAcceleration = new float[3];
    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_LINEAR_ACCELERATION) {
            System.arraycopy(sensorEvent.values, 0, linearAcceleration, 0, sensorEvent.values.length);
            timeStamp = sensorEvent.timestamp;
            notifyAccelerationObserver();
        }
    }

    private void notifyAccelerationObserver() {
        for (LinearAccelerationOfficialObserver a : observersLinearAccelerationOfficial) {
            a.onLinearAccelerationOfficialSensorChanged(this.linearAcceleration, this.timeStamp);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }

}
