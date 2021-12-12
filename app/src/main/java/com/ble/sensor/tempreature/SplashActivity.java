package com.ble.sensor.tempreature;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.anggrayudi.storage.SimpleStorageHelper;
import com.anggrayudi.storage.permission.ActivityPermissionRequest;
import com.anggrayudi.storage.permission.PermissionCallback;
import com.anggrayudi.storage.permission.PermissionReport;
import com.anggrayudi.storage.permission.PermissionResult;
import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import org.jetbrains.annotations.NotNull;
import no.nordicsemi.android.support.v18.scanner.BluetoothLeScannerCompat;
import java.util.List;

public class SplashActivity extends AppCompatActivity {
    private ConstraintLayout bgScan, btnScan;
    private Handler startHandler = new Handler();
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private BluetoothLeScanner mBluetoothLeScanner;
    private BluetoothLeScannerCompat scanner;
    private ScanSettings scanSettings;
    private CircularProgressBar progressBar;
    private Handler mHandler;
    boolean scanning = false;
    private final int SCAN_PERIOD = 5000;
    private final String TAG = "Splash";
    private ActivityResultLauncher<Intent> splashActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        // There are no request codes
                        Intent data = result.getData();
//                        addLog("bluetooth turn callback");
                        if (data != null) {
                            checkBluetooth();
                        }
                    }
                }
            });

    private final ActivityPermissionRequest permissionRequest = new ActivityPermissionRequest.Builder(this)
            .withPermissions(Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION)
            .withCallback(new PermissionCallback() {
                @Override
                public void onPermissionsChecked(@NotNull PermissionResult result, boolean fromSystemDialog) {
                    String grantStatus = result.getAreAllPermissionsGranted() ? getString(R.string.permission_granted) : getString(R.string.permission_denied);
                    Toast.makeText(getBaseContext(), grantStatus, Toast.LENGTH_SHORT).show();
                    if (result.getAreAllPermissionsGranted()) {
                        checkBluetooth();
                    } else {
                        permissionRequest.check();
                    }
                }

                @Override
                public void onShouldRedirectToSystemSettings(@NotNull List<PermissionReport> blockedPermissions) {
                    SimpleStorageHelper.redirectToSystemSettings(SplashActivity.this);
                }
            })
            .build();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        btnScan = findViewById(R.id.btnscan);
        bgScan = findViewById(R.id.bg_scan);
//        Util.initLogWriter();
    }

    @Override
    protected void onStart() {
        super.onStart();
        startHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                checkpermission();
            }
        }, 2000);
    }

    @Override
    protected void onStop() {
        super.onStop();
//        Util.logWriterClose();
    }

    public void initScanner() {
//        addLog("init scanner");
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mHandler = new Handler();
        mBluetoothLeScanner = BluetoothAdapter.getDefaultAdapter().getBluetoothLeScanner();
        scanner = BluetoothLeScannerCompat.getScanner();
        progressBar = findViewById(R.id.progressBar);
        progressBar.setProgress(65f);
        progressBar.setProgressWithAnimation(65f, Long.valueOf(1000)); // =1s
        progressBar.setProgressMax(200f);
        progressBar.setProgressDirection(CircularProgressBar.ProgressDirection.TO_RIGHT);
        progressBar.setProgressBarWidth(8f);
        progressBar.setBackgroundProgressBarWidth(20f);
        progressBar.setIndeterminateMode(true);
        progressBar.setRoundBorder(true);
        scanSettings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_BALANCED)
                .build();
    }

    public void updateUi() {
        try {
            bgScan.setBackgroundResource(scanning ? R.drawable.ic_stopscan : R.drawable.ic_scanback);
            btnScan.setSelected(!scanning);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void ToggleScan(View view)
    {
        if (scanning) {
//            addLog("stop scan");
            mBluetoothLeScanner.stopScan(mLeScanCallback);
            scanning = false;
            progressBar.setVisibility(View.GONE);
        } else {
//            addLog("start scan");
            progressBar.setVisibility(View.VISIBLE);
            mBluetoothLeScanner.startScan(null, scanSettings, mLeScanCallback);
            scanning = true;
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mBluetoothLeScanner.stopScan(mLeScanCallback);
                    scanning = false;
                    updateUi();
                    progressBar.setVisibility(View.GONE);
                }
            }, SCAN_PERIOD);
        }
        updateUi();
    }

    public void checkpermission() {
//        addLog("check permission");
        permissionRequest.check();
    }

    public void checkBluetooth(){
//        addLog("Check Bluetooth");
        bluetoothManager =(BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        if(mBluetoothAdapter == null){
            Toast.makeText(this,"Device does not support bluetooth",Toast.LENGTH_SHORT).show();
            return;
        }
        else if(!mBluetoothAdapter.isEnabled()){
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            splashActivityResultLauncher.launch(enableBtIntent);
            return;
        }
        else{
            initScanner();
            btnScan.setVisibility(View.VISIBLE);
        }
    }

    public void OpenDevice(BluetoothDevice WearDev) {
//        addLog("Start main with device information");
        if (scanning) {
            mBluetoothLeScanner.stopScan(mLeScanCallback);
            scanning = false;
        }
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("name", WearDev.getName());
        intent.putExtra("address", WearDev.getAddress());
        startActivity(intent);
    }

    private void manageScanResult(ScanResult result) {
//        addLog("Found device in onScanResult");
        BluetoothDevice device = result.getDevice();
        String address = device.getAddress();
        String devicename = result.getDevice().getName();
//        addLog("name from result " + devicename);
        if (devicename == null) {
            devicename = result.getScanRecord().getDeviceName();
//            addLog("name from record " + devicename);
        }
//        addLog("Bluetooth Device Found. Name:" + devicename + " Address:" + address);
        boolean isnew = true;

        if (devicename != null && devicename.equals("WearDev")) {
//            addLog("WearDev Sensor Device Found." + " Address:" + address);
            Toast.makeText(this, "Sensor Found, auto connecting...", Toast.LENGTH_SHORT).show();
            OpenDevice(device);
        }
    }

    private final ScanCallback mLeScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
            super.onScanResult(callbackType, result);
            manageScanResult(result);
        }

        @Override
        public void onBatchScanResults(@NonNull List<ScanResult> results) {
//            addLog("Batch mode" + results.size());
            for (ScanResult result : results) {
                manageScanResult(result);
            }
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode) {
//            addLog("Scan Failed. error code:" + errorCode);
            super.onScanFailed(errorCode);
        }
    };

//    public void addLog(String log){
//        Util.printLog(log);
//    }
}