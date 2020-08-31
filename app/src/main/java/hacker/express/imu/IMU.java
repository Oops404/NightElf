package hacker.express.imu;

public class IMU {
    private static final double KP = 100;
    private static final double KI = 0.002;
    private static final double HALF_T = 0.001;


    public static EulerAngles update(double ax, double ay, double az, double gx, double gy, double gz) {
        double seq0 = 1, seq1 = 0, seq2 = 0, seq3 = 0;
        int exInt = 0, eyInt = 0, ezInt = 0;

        double norm = Math.sqrt(ax * ax + ay * ay + az * az);

        double vx = 2 * (seq1 * seq3 - seq0 * seq2);
        double vy = 2 * (seq0 * seq1 + seq2 * seq3);
        double vz = seq0 * seq0 - seq1 * seq1 - seq2 * seq2 + seq3 * seq3;

        double ex = (ay * vz - az * vy);
        double ey = (az * vx - ax * vz);
        double ez = (ax * vy - ay * vx);

        //积分误差比例积分增益
        exInt += ex * KI;
        eyInt += ey * KI;
        ezInt += ez * KI;

        gx += KP * ex + exInt;
        gy += KP * ey + eyInt;
        gz += KP * ez + ezInt;

        seq0 += (-seq1 * gx - seq2 * gy - seq3 * gz) * HALF_T;
        seq1 += (seq0 * gx + seq2 * gz - seq3 * gy) * HALF_T;
        seq2 += (seq0 * gy - seq1 * gz + seq3 * gx) * HALF_T;
        seq3 += (seq0 * gz + seq1 * gy - seq2 * gx) * HALF_T;

        norm = Math.sqrt(seq0 * seq0 + seq1 * seq1 + seq2 * seq2 + seq3 * seq3);
        seq0 /= norm;
        seq1 /= norm;
        seq2 /= norm;
        seq3 /= norm;
        return new EulerAngles(
                Math.asin(-2 * seq1 * seq3 + 2 * seq0 * seq2) * 57.3,
                Math.atan2(2 * seq2 * seq3 + 2 * seq0 * seq1, -2 * seq1 * seq1 - 2 * seq2 * seq2 + 1) * 57.3,
                Math.atan2(2 * (seq1 * seq2 + seq0 * seq3), seq0 * seq0 + seq1 * seq1 - seq2 * seq2 - seq3 * seq3) * 57.3
        );
    }

}
