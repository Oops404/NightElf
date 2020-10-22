package hacker.express.sensor.observer;

public interface OrientationAnglesSensorObserver {

    void onOrientationAnglesSensorChanged(float[] orientationAngles, long timeStamp);

}
