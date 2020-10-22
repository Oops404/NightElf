package hacker.express.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.PowerManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;

import hacker.express.R;
import hacker.express.service.ElfService;

public class NightElfActivity extends Activity {
    private final int WRITE_EXTERNAL_STORAGE_PERMISSION_FLAG = 1;
    private static PowerManager.WakeLock mWakeLock;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        acquireWakeLock(this);
        setContentView(R.layout.layout_night_elf);

        askPermissions();
        startMonitor();
    }

    protected void askPermissions() {
        int permission = ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE);

        if (permission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.MOUNT_UNMOUNT_FILESYSTEMS,
                            Manifest.permission.DISABLE_KEYGUARD
                    },
                    WRITE_EXTERNAL_STORAGE_PERMISSION_FLAG);
        } else {
            moveTaskToBack(true);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == WRITE_EXTERNAL_STORAGE_PERMISSION_FLAG) {
            boolean has = ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
            if (!has) {
                askPermissions();
            } else {
                moveTaskToBack(true);
                startMonitor();
            }
        }
    }

    private void startMonitor() {
        // IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_ON);
        // filter.addAction(Intent.ACTION_SCREEN_OFF);
        Intent intent = new Intent(NightElfActivity.this, ElfService.class);
        intent.setAction("android.intent.action.RESPOND_VIA_MESSAGE");
        NightElfActivity.this.startService(intent);
        Log.w("[TEST]", " startMonitor START");
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

    private boolean initStart = true;

    @Override
    protected void onStart() {
        System.out.println("onStart");
        super.onStart();
    }

    @Override
    protected void onResume() {
        System.out.println("onResume");
        super.onResume();
        try {
            if (!initStart) {
                moveTaskToBack(true);
            }
        } finally {
            initStart = false;
        }
    }

    @Override
    protected void onRestart() {
        System.out.println("onRestart");
        super.onRestart();
        moveTaskToBack(true);
    }

    @Override
    protected void onPause() {
        System.out.println("onPause");
        super.onPause();
        //moveTaskToBack(true);
    }

    @Override
    protected void onStop() {
        System.out.println("onStop");
        super.onStop();
        //moveTaskToBack(true);
    }

    @Override
    protected void onDestroy() {
        System.out.println("onDestroy");
        super.onDestroy();
        moveTaskToBack(true);
    }

}
