package hacker.express.sensor;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;

import java.util.ArrayList;

import hacker.express.sensor.observer.RotationVectorSensorObserver;

public class RotationVectorSensor implements SensorEventListener {

    private static final String tag = RotationVectorSensor.class.getSimpleName();

    private ArrayList<RotationVectorSensorObserver> rotationVectorSensorObservers;
    private Context context;

    private float[] rotation = new float[5];

    private long timeStamp = 0;
    private SensorManager sensorManager;

    public RotationVectorSensor(Context context) {
        this.context = context;
        this.rotationVectorSensorObservers = new ArrayList<>();
        sensorManager = (SensorManager) this.context
                .getSystemService(Context.SENSOR_SERVICE);
    }

    public void registerRotationVectorObserver(RotationVectorSensorObserver observer) {
        if (rotationVectorSensorObservers.size() == 0) {
            sensorManager.registerListener(this,
                    sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR),
                    SensorManager.SENSOR_DELAY_FASTEST);
        }
        int index = rotationVectorSensorObservers.indexOf(observer);
        if (index == -1) {
            rotationVectorSensorObservers.add(observer);
        }
    }

    public void removeGravityObserver(RotationVectorSensorObserver observer) {
        int index = rotationVectorSensorObservers.indexOf(observer);
        if (index >= 0) {
            rotationVectorSensorObservers.remove(index);
        }
        if (rotationVectorSensorObservers.size() == 0) {
            sensorManager.unregisterListener(this);
        }
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        if(sensorEvent.sensor.getType()==Sensor.TYPE_ROTATION_VECTOR){
            System.arraycopy(sensorEvent.values, 0, rotation, 0, sensorEvent.values.length);
            timeStamp = sensorEvent.timestamp;
        }
        notifyRotationVectorObserver();
    }

    private void notifyRotationVectorObserver() {
        for (RotationVectorSensorObserver a : rotationVectorSensorObservers) {
            a.onRotationVectorSensorChanged(this.rotation, this.timeStamp);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
