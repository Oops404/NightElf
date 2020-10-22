package hacker.express.sensor.observer;

public interface RotationVectorSensorObserver {

    void onRotationVectorSensorChanged(float[] rotation, long timeStamp);

}
