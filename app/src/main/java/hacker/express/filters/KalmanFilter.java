package hacker.express.filters;

import java.util.ArrayList;

public class KalmanFilter {

    private final double Q = 0.000001;
    private final double R = 0.001;
    private ArrayList<Double> dataArrayList;
    private int length;

    private double data[];
    private double xHat[];
    private double xHatMinus[];
    private double p[];
    private double pMinus[];
    private double k[];

    public KalmanFilter(ArrayList<Double> arrayList) {
        this.dataArrayList = arrayList;
        this.length = arrayList.size();
        data = new double[length];
        xHat = new double[length];
        xHatMinus = new double[length];
        p = new double[length];
        pMinus = new double[length];
        k = new double[length];
        xHat[0] = 0;
        p[0] = 1.0;

        for (int i = 0; i < length; i++) {
            data[i] = (double) dataArrayList.get(i);
        }
    }

    public ArrayList<Double> calc() {
        if (dataArrayList.size() < 2) {
            return dataArrayList;
        }
        for (int n = 1; n < length; n++) {
            xHatMinus[n] = xHat[n - 1];
            pMinus[n] = p[n - 1] + Q;
            k[n] = pMinus[n] / (pMinus[n] + R);
            xHat[n] = xHatMinus[n] + k[n] * (data[n] - xHatMinus[n]);
            p[n] = (1 - k[n]) * pMinus[n];
        }

        for (int i = 0; i < length; i++) {
            dataArrayList.set(i, xHat[i]);
        }
        dataArrayList.remove(0);
        return dataArrayList;
    }

}
