package hacker.express.imu;

public class EulerAngles {
    private double pitch;
    private double roll;
    private double yaw;

    public EulerAngles(double pitch, double roll, double yaw) {
        this.pitch = pitch;
        this.roll = roll;
        this.yaw = yaw;
    }

    public double getPitch() {
        return pitch;
    }

    public void setPitch(double pitch) {
        this.pitch = pitch;
    }

    public double getRoll() {
        return roll;
    }

    public void setRoll(double roll) {
        this.roll = roll;
    }

    public double getYaw() {
        return yaw;
    }

    public void setYaw(double yaw) {
        this.yaw = yaw;
    }

    @Override
    public String toString() {
        return "EulerAngles{" +
                "pitch=" + pitch +
                ", roll=" + roll +
                ", yaw=" + yaw +
                '}';
    }
}
