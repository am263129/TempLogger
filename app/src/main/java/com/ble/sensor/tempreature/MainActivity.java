package com.ble.sensor.tempreature;

import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;

import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.mikhaellopez.circularprogressbar.CircularProgressBar;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    EditText ticket, tempRectal, tempRoom, variety, note;
    TextView tempValue;
    Spinner type, hairLen, hairType;
    Dialog configDlg;
    CircularProgressBar circularProgressBar;
    ImageView icoConnect;
    ConstraintLayout btnConnect, connectingProgress;
    private BufferedWriter csvWriter = null;
    private final String csvFileName = "petvoice_temp_log.csv";
    private String csvFilePath = null;
    private final String TAG = "MainActivity";
//    private int deviceWidth;
    private BluetoothDevice wearDev;
    private String sensorAddress;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothManager bluetoothManager;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothGattCharacteristic tempData;
    private Handler connectHandler = new Handler();
    private int connectionStatus = BluetoothProfile.STATE_DISCONNECTED;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sensorAddress = getIntent().getStringExtra("address") == null ? "" : getIntent().getStringExtra("address");
//        Util.printLog("Address :" + sensorAddress);
        initView();
        updateUI();
        initBluetooth();
        // test
//        connect();
//        enableUi(true);
    }

    public void initView() {
        setContentView(R.layout.activity_main);
        ticket = findViewById(R.id.valueTicket);
        tempRectal = findViewById(R.id.valueTempRectal);
        tempRoom = findViewById(R.id.valueTempRoom);
        type = findViewById(R.id.spinType);
        hairLen = findViewById(R.id.spinHairLen);
        hairType = findViewById(R.id.spinHairType);
        variety = findViewById(R.id.valueVariety);
        note = findViewById(R.id.valueNote);
        icoConnect = findViewById(R.id.ico_connect);
        btnConnect = findViewById(R.id.btn_connect);
        connectingProgress = findViewById(R.id.connectingProgress);
        tempValue = findViewById(R.id.tempValue);
        configDlg = new Dialog(this);
        configDlg.setContentView(R.layout.dlg_connect);
        circularProgressBar = findViewById(R.id.circularProgressBar);
        circularProgressBar.setProgress(65f);
        circularProgressBar.setProgressWithAnimation(65f, Long.valueOf(1000)); // =1s
        circularProgressBar.setProgressMax(200f);
        circularProgressBar.setProgressDirection(CircularProgressBar.ProgressDirection.TO_RIGHT);
        circularProgressBar.setProgressBarWidth(8f);
        circularProgressBar.setBackgroundProgressBarWidth(20f);
        circularProgressBar.setIndeterminateMode(true);
        circularProgressBar.setRoundBorder(true);
//        Util.printLog("UI initiated");
    }

    public void initBluetooth() {
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        try {
            wearDev = mBluetoothAdapter.getRemoteDevice(sensorAddress);
//            Util.printLog("Device address : " + sensorAddress);
            if (wearDev == null) {
//                Util.printLog("No Ble device (WearDev)");
                Toast.makeText(this, getString(R.string.msg_no_sensor_device), Toast.LENGTH_SHORT).show();
                finish();
            } else {
//                Util.printLog("Check connection status :"+connectionStatus);
                if (connectionStatus != BluetoothProfile.STATE_CONNECTED)
                {
//                    Util.printLog("Connect device");
                    connect();
                }
            }
        } catch (Exception e) {
            showToast(R.string.msg_invalidAddress);
            connectingProgress.setVisibility(View.GONE);
//            Util.printLog("Invalid device address");
            e.printStackTrace();
        }
//        Util.printLog("Bluetooth initiated");
    }


    @Override
    protected void onStart() {
        super.onStart();
//        Util.initLogWriter();
        initCSV();
//        Util.printLog("Start App");
        if (wearDev != null && connectionStatus == BluetoothProfile.STATE_DISCONNECTED)
        {
            connect();
        }
    }

    @Override
    protected void onStop() {
//        Util.printLog("Stop App");
        super.onStop();
//        Util.logWriterClose();
        stopReadTempData();
        try {
            csvWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void updateUI() {
        connectingProgress.setVisibility(
                connectionStatus == BluetoothProfile.STATE_CONNECTED ||
                connectionStatus == BluetoothProfile.STATE_DISCONNECTED ? View.GONE : View.VISIBLE
        );
//        enableUi(connectionStatus == BluetoothProfile.STATE_CONNECTED);
    }

//    public void enableUi(boolean enabled) {
//        Util.printLog("enable UI:" + enabled);
//        ticket.setEnabled(enabled);
//        type.setEnabled(enabled);
//        variety.setEnabled(enabled);
//        hairLen.setEnabled(enabled);
//        hairType.setEnabled(enabled);
//        tempRectal.setEnabled(enabled);
//        tempRoom.setEnabled(enabled);
//        note.setEnabled(enabled);
//    }

    public void clear(View view) {
//        Util.printLog("Clear data");
        ticket.setText("");
        type.setSelection(0);
        variety.setText("");
        hairLen.setSelection(0);
        hairType.setSelection(0);
        tempRectal.setText("");
        tempRoom.setText("");
        note.setText("");
    }

    public void connect() {
        if (connectionStatus == BluetoothProfile.STATE_CONNECTED) {
//            Util.printLog("device disconnecting");
            stopReadTempData();
            mBluetoothGatt.disconnect();
            tempData = null;
            connectionStatus = BluetoothProfile.STATE_DISCONNECTING;
        } else if (connectionStatus == BluetoothProfile.STATE_DISCONNECTED) {
//            Util.printLog("device connecting");
            connectionStatus = BluetoothProfile.STATE_CONNECTING;
            // test
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mBluetoothGatt = wearDev.connectGatt(this, false, mGattCallback, BluetoothDevice.TRANSPORT_LE);
            } else {
                mBluetoothGatt = wearDev.connectGatt(this, false, mGattCallback);
            }
            connectHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //If still connecting after 5 sec, update ui, and show as unable to connect;
                    if (connectionStatus == BluetoothProfile.STATE_CONNECTING) {
//                        Util.printLog("device doesn't response");
                        connectionStatus = BluetoothProfile.STATE_DISCONNECTED;
                        updateUI();
                        showToast(R.string.msg_connectionFailed);
                    }
                }
            }, 8000);
        } else {
//            Util.printLog("device is busy");
            showToast(R.string.msg_device_busy);
        }
        updateUI();
    }

    public void startReadTempData(BluetoothGattCharacteristic tempData) {
//        Util.printLog("set temp data notify enable");
        boolean result = mBluetoothGatt.setCharacteristicNotification(tempData, true);
//        Util.printLog("set setCharacteristicNotification result: " + result);
        UUID uuid = UUID.fromString(SensorUUID.CLIENT_CHARACTERISTIC_CONFIG);
        BluetoothGattDescriptor descriptor = tempData.getDescriptor(uuid);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE);
            boolean descriptor_result = mBluetoothGatt.writeDescriptor(descriptor);
//            Util.printLog("Set writeDescriptor result(enable): " + descriptor_result);
        } else {
//            Util.printLog("Undefined descriptor");
        }
    }

    public void stopReadTempData() {
        if (tempData == null) {
//            Util.printLog("undefined tempData");
            return;
        }
        boolean result = mBluetoothGatt.setCharacteristicNotification(tempData, false);
//        Util.printLog("set setCharacteristicNotification result: " + result);
        UUID uuid = UUID.fromString(SensorUUID.CLIENT_CHARACTERISTIC_CONFIG);
        BluetoothGattDescriptor descriptor = tempData.getDescriptor(uuid);
        if (descriptor != null) {
            descriptor.setValue(BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE);
            boolean descriptor_result = mBluetoothGatt.writeDescriptor(descriptor);
//            Util.printLog("Set writeDescriptor result(disable): " + descriptor_result);
        } else {
//            Util.printLog("Descriptor of MEMS_DATA null");
        }
    }


    public void dspReadCharacteristic(byte[] bytes) {
//        Util.printLog("Read result : "+Util.bytesToHex(bytes));
        if(bytes.length<21){
//            Util.printLog("No enough data length :" + bytes.length);
            return;
        }
        float temp = Util.getTemperature(bytes);
        //float temp = Util.Half(bytes, 19);
//        Util.printLog("Temperature :" + String.format("%.2f",temp));
        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                tempValue.setText(String.format("%.2f",temp) + getString(R.string.symbolCelsius));
            }
        });

    }


    public void save(View view) {
        //test
        if (validate()) {
            saveToCsv();
        }
    }

    public void initCSV() {
        File mediaStorageDir = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS), "PVLogger");
        csvFilePath = mediaStorageDir.getPath() + File.separator + csvFileName;
        try {
            boolean writeHeader = false;
            if(!new File(csvFilePath).exists()){
                writeHeader = true;
                mediaStorageDir.mkdirs();
            }
            csvWriter = new BufferedWriter(new FileWriter(csvFilePath,true));
            if (writeHeader) {
                csvWriter.append("日時,診察券番号,犬 or 猫,品種,被毛長さ,被毛種類,直腸温,首周り温度,室温,メモ");
                csvWriter.newLine();
            }
        }catch (Exception e){
            csvWriter = null;
            e.printStackTrace();
            showToast(R.string.permission_denied);
        }
    }

    public void saveToCsv() {
        if (csvWriter == null) {
            initCSV();
        }
        try {
            Calendar calendar = Calendar.getInstance();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss");
            String time = simpleDateFormat.format(calendar.getTime());
            csvWriter.append(time); // Date time
            csvWriter.append(",");
            csvWriter.append(ticket.getText().toString()); //Ticket
            csvWriter.append(",");
            csvWriter.append(getResources().getStringArray(R.array.petType)[type.getSelectedItemPosition()]); //Type
            csvWriter.append(",");
            csvWriter.append(variety.getText().toString()); //Variety
            csvWriter.append(",");
            csvWriter.append(getResources().getStringArray(R.array.HairLen)[hairLen.getSelectedItemPosition()]); // Hair length
            csvWriter.append(",");
            csvWriter.append(getResources().getStringArray(R.array.HairType)[hairType.getSelectedItemPosition()]); // Hair type
            csvWriter.append(",");
            csvWriter.append(tempRectal.getText().toString()); // Rectal temperature
            csvWriter.append(",");
            csvWriter.append(tempValue.getText().toString()
                    .replace(getString(R.string.symbolCelsius),"")
                    .replace("--","")); // Neck temperature
            csvWriter.append(",");
            csvWriter.append(tempRoom.getText().toString());  // Room temperature
            csvWriter.append(",");
            csvWriter.append(note.getText().toString().trim()); // Note  /***Fix line break/
            csvWriter.newLine();
            showToast(R.string.msg_filesaved);
        } catch (Exception E) {
            E.printStackTrace();
        }
    }

    public boolean validate() {
        if (ticket.getText().toString().isEmpty()) {
            showToast(R.string.msgEmptyTicket);
            return false;
        } else if (tempRectal.getText().toString().isEmpty()) {
            showToast(R.string.msgEmptyRectalTemp);
            return false;
        } else if (tempRoom.getText().toString().isEmpty()) {
            showToast(R.string.msgEmptyRoomTemp);
            return false;
        }
        return true;
    }

    public void showToast(int stringId) {
        Toast.makeText(this, getString(stringId), Toast.LENGTH_SHORT).show();
    }

    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {

            if (newState == BluetoothProfile.STATE_CONNECTED) {
//                Util.printLog("device connected");
                boolean retVal = mBluetoothGatt.discoverServices();
                // Attempts to discover services after successful connection.
//                Util.printLog("getting service list" + retVal);
                connectionStatus = BluetoothProfile.STATE_CONNECTED;
            } else if (newState == BluetoothGatt.GATT_INSUFFICIENT_AUTHENTICATION) {
//                Util.printLog("Device is unable to communicate due to unpaired");
//                if (gatt.getDevice().getBondState() == BluetoothDevice.BOND_NONE) {
                    // The broadcast receiver should be called.
//                    Util.printLog("Request Bond");
//                } else {
//                    Util.printLog("already bond");
//                }
            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//                if (connectionStatus == BluetoothProfile.STATE_CONNECTING || connectionStatus == BluetoothProfile.STATE_DISCONNECTED) {
//                    Util.printLog("connecting failed");
//                } else
//                    Util.printLog("device disconnected");
                connectionStatus = BluetoothProfile.STATE_DISCONNECTED;
            } else if (newState == BluetoothProfile.STATE_CONNECTING) {
//                Util.printLog("device connecting");
            }
            MainActivity.this.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateUI();
                }
            });
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
//            Util.printLog("Service Discovered" + status);
            if (status == BluetoothGatt.GATT_SUCCESS) {
                List<BluetoothGattService> bluetoothGattServices = gatt.getServices();
//                Util.printLog(bluetoothGattServices.size() + " " + "Services found");
                for (BluetoothGattService service : bluetoothGattServices) {
                    String newService = SensorUUID.lookup(service.getUuid().toString(), service.getUuid().toString());
//                    Util.printLog("Service" +
//                            " UUID " + newService);
                    for (BluetoothGattCharacteristic characteristic : service.getCharacteristics()) {
                        String name = SensorUUID.lookup(characteristic.getUuid().toString(), characteristic.getUuid().toString());
//                        Util.printLog("Characteristic UUID : "+name);
                        if (name.equals("MEMS_DATA")) {
                            tempData = characteristic;
//                            Util.printLog("Sensor device is ready");
                            startReadTempData(characteristic);
                        }
                    }
//                    Util.printLog("End check service.");
                }
            } else {
//                Util.printLog("getting service failed");
            }
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt,
                                         BluetoothGattCharacteristic characteristic,
                                         int status) {
//            if (status == BluetoothGatt.GATT_SUCCESS) {
//                Util.printLog("Read data:" + Util.bytesToHex(characteristic.getValue()));
//                if (SensorUUID.lookup(characteristic.getUuid().toString(), characteristic.getUuid().toString()).equals("MEMS_CONF")) {
//                    Util.printLog("Read setting" + Util.bytesToHex(characteristic.getValue()));
//                }
//            } else {
//                Util.printLog("Reading data failed");
//            }
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt,
                                            BluetoothGattCharacteristic characteristic) {
//            Util.printLog("data received from change listener:" + Util.bytesToHex(characteristic.getValue()));
            if (SensorUUID.lookup(characteristic.getUuid().toString(), characteristic.getUuid().toString()).equals("MEMS_DATA")) {
//                Util.printLog("MEMS_DATA : " + Util.bytesToHex(characteristic.getValue()));
                dspReadCharacteristic(characteristic.getValue());
            }
        }

        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            super.onDescriptorWrite(gatt, descriptor, status);
//            if (status == BluetoothGatt.GATT_SUCCESS) {
                // stop/start record comes here. and disconnect will also comes here.
//            }
//            Util.printLog("");
        }
    };
}