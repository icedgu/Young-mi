/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.google.android.gms.samples.vision.face.googlyeyes;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.media.Image;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.text.method.HideReturnsTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;

public class LinkingActivity extends Activity implements RadioGroup.OnCheckedChangeListener {
    private static final int REQUEST_SELECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    public static final String TAG = "YoungME";
    private static final int UART_PROFILE_CONNECTED = 20;
    private static final int UART_PROFILE_DISCONNECTED = 21;
    private String Pedo;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 99;
    private int mState = UART_PROFILE_DISCONNECTED;
    private UartService mService = null;
    private BluetoothDevice mDevice = null;
    private BluetoothAdapter mBtAdapter = null;

    private Button btnConnectDisconnect;
    private ImageButton CalBtn,CalSet;
    private ImageView HRStenview,HRSoneview;
    private ImageView Pedotenview, Pedooneview;
    private ImageView Caltenview, Caloneview;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main_ble);

        mBtAdapter = BluetoothAdapter.getDefaultAdapter();
        if (mBtAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnConnectDisconnect = (Button) findViewById(R.id.Connectbtn);

        HRSoneview = (ImageView)findViewById(R.id.HRS_one);
        HRStenview = (ImageView)findViewById(R.id.HRS_ten);

        Pedooneview = (ImageView)findViewById(R.id.Walk_one);
        Pedotenview = (ImageView)findViewById(R.id.Walk_ten);

        Caloneview = (ImageView)findViewById(R.id.Cal_one);
        Caltenview = (ImageView)findViewById(R.id.Cal_ten);

        service_init();

        btnConnectDisconnect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mBtAdapter.isEnabled()) {
                    Log.i(TAG, "onClick - BT not enabled yet");
                    Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
                } else {
                    if (btnConnectDisconnect.getText().equals("Connect")) {
                        Intent newIntent = new Intent(LinkingActivity.this, DeviceListActivity.class);
                        startActivityForResult(newIntent, REQUEST_SELECT_DEVICE);

                    } else {
                        //Disconnect button pressed
                        if (mDevice != null) {
                            mService.disconnect();
                        }
                    }
                }
            }
        });
        CalSet = (ImageButton)findViewById(R.id.walk);
        CalSet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String m = "5508080200301800700046";
                byte[] M = hexStringToByteArray(m);
                mService.writeRXCharacteristic(M);
            }
        });
        CalBtn = (ImageButton)findViewById(R.id.btnCalorie);
        CalBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String request = "55020800F6";
                byte[] R = hexStringToByteArray(request);
                mService.writeRXCharacteristic(R);
            }
        });
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String byteArrayToHexString(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02X", b & 0xff));
        }
        return sb.toString();
    }

    private ServiceConnection mServiceConnection = new ServiceConnection() {
        public void onServiceConnected(ComponentName className, IBinder rawBinder) {
            mService = ((UartService.LocalBinder) rawBinder).getService();
            Log.d(TAG, "onServiceConnected mService= " + mService);
            if (!mService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        public void onServiceDisconnected(ComponentName classname) {
            mService = null;
        }
    };

    private Handler mHandler = new Handler() {
        @Override
        //Handler events that received from UART service
        public void handleMessage(Message msg) {
        }
    };

    private final BroadcastReceiver UARTStatusChangeReceiver = new BroadcastReceiver() {

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            final Intent mIntent = intent;

            if (action.equals(UartService.ACTION_GATT_CONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Log.d(TAG, "UART_CONNECT_MSG");
                        btnConnectDisconnect.setText("Disconnect");
                        mState = UART_PROFILE_CONNECTED;
                    }
                });
            }

            if (action.equals(UartService.ACTION_GATT_DISCONNECTED)) {
                runOnUiThread(new Runnable() {
                    public void run() {
                        Log.d(TAG, "UART_DISCONNECT_MSG");
                        btnConnectDisconnect.setText("Connect");
                        //HRSview.setText("");
                        //Pedoview.setText("");
                        mState = UART_PROFILE_DISCONNECTED;
                        mService.close();
                        //setUiState();
                    }
                });
            }

            if (action.equals(UartService.ACTION_GATT_SERVICES_DISCOVERED)) {
                mService.enableTXNotification();
            }

            if (action.equals(UartService.ACTION_DATA_AVAILABLE)) {
                final byte[] txValue = intent.getByteArrayExtra(UartService.EXTRA_DATA);
                runOnUiThread(new Runnable() {
                    public void run() {
                        try {
                            String text = byteArrayToHexString(txValue);
                            String RX = returnService(text);

                            if (text.substring(4, 6).equals("03")) { //HRS
                                setHRSnum(RX);
                                int HRS_check = Integer.parseInt(RX);
                                if(HRS_check < 50){
                                    String vib = "55050002050208EA";
                                    byte[] value = hexStringToByteArray(vib);
                                    mService.writeRXCharacteristic(value);
                                }
                            } else if (text.substring(4, 6).equals("02") || text.substring(4, 6).equals("08")) {
                                if(text.substring(4, 6).equals("02")) { //WALK
                                    setWalknum(RX);
                                    setCalnum("11");
                                    Pedo = RX;
                                }
                                else if (text.substring(4, 6).equals("08")){ //calorie 구하기
                                    int cal;
                                    cal = returnCalorie(text,Pedo);
                                    String calorie = Integer.toString(cal);
                                    setCalnum(calorie);
                                }
                            }
                        } catch (Exception e) {
                            Log.e(TAG, e.toString());
                        }
                    }
                });

            }

            if (action.equals(UartService.DEVICE_DOES_NOT_SUPPORT_UART)) {
                showMessage("Device doesn't support UART. Disconnecting");
                mService.disconnect();
            }
        }
    };

    public void setCalnum(String RX) {

        int num = Integer.parseInt(RX);
        String ten = String.valueOf(num / 10);
        String one = String.valueOf(num % 10);

        switch (ten) {
            case "0":
                break;
            case "1":
                Caltenview.setImageResource(R.drawable.n_1);
                break;
            case "2":
                Caltenview.setImageResource(R.drawable.n_2);
                break;
            case "3":
                Caltenview.setImageResource(R.drawable.n_3);
                break;
            case "4":
                Caltenview.setImageResource(R.drawable.n_4);
                break;
            case "5":
                Caltenview.setImageResource(R.drawable.n_5);
                break;
            case "6":
                Caltenview.setImageResource(R.drawable.n_6);
                break;
            case "7":
                Caltenview.setImageResource(R.drawable.n_7);
                break;
            case "8":
                Caltenview.setImageResource(R.drawable.n_8);
                break;
            case "9":
                Caltenview.setImageResource(R.drawable.n_9);
                break;
        }

        switch (one) {
            case "0":
                Caltenview.setImageResource(R.drawable.n_0);
                break;
            case "1":
                Caltenview.setImageResource(R.drawable.n_1);
                break;
            case "2":
                Caltenview.setImageResource(R.drawable.n_2);
                break;
            case "3":
                Caltenview.setImageResource(R.drawable.n_3);
                break;
            case "4":
                Caltenview.setImageResource(R.drawable.n_4);
                break;
            case "5":
                Caltenview.setImageResource(R.drawable.n_5);
                break;
            case "6":
                Caltenview.setImageResource(R.drawable.n_6);
                break;
            case "7":
                Caltenview.setImageResource(R.drawable.n_7);
                break;
            case "8":
                Caltenview.setImageResource(R.drawable.n_8);
                break;
            case "9":
                Caltenview.setImageResource(R.drawable.n_9);
                break;
        }
    }

    public void setWalknum(String RX) {

        int num = Integer.parseInt(RX);
        String ten = String.valueOf(num / 10);
        String one = String.valueOf(num % 10);

        switch (ten) {
            case "0":
                break;
            case "1":
                Pedotenview.setImageResource(R.drawable.n_1);
                break;
            case "2":
                Pedotenview.setImageResource(R.drawable.n_2);
                break;
            case "3":
                Pedotenview.setImageResource(R.drawable.n_3);
                break;
            case "4":
                Pedotenview.setImageResource(R.drawable.n_4);
                break;
            case "5":
                Pedotenview.setImageResource(R.drawable.n_5);
                break;
            case "6":
                Pedotenview.setImageResource(R.drawable.n_6);
                break;
            case "7":
                Pedotenview.setImageResource(R.drawable.n_7);
                break;
            case "8":
                Pedotenview.setImageResource(R.drawable.n_8);
                break;
            case "9":
                Pedotenview.setImageResource(R.drawable.n_9);
                break;
        }

        switch (one) {
            case "0":
                Pedooneview.setImageResource(R.drawable.n_0);
                break;
            case "1":
                Pedooneview.setImageResource(R.drawable.n_1);
                break;
            case "2":
                Pedooneview.setImageResource(R.drawable.n_2);
                break;
            case "3":
                Pedooneview.setImageResource(R.drawable.n_3);
                break;
            case "4":
                Pedooneview.setImageResource(R.drawable.n_4);
                break;
            case "5":
                Pedooneview.setImageResource(R.drawable.n_5);
                break;
            case "6":
                Pedooneview.setImageResource(R.drawable.n_6);
                break;
            case "7":
                Pedooneview.setImageResource(R.drawable.n_7);
                break;
            case "8":
                Pedooneview.setImageResource(R.drawable.n_8);
                break;
            case "9":
                Pedooneview.setImageResource(R.drawable.n_9);
                break;
        }
    }

            public void setHRSnum(String RX) {

        int num = Integer.parseInt(RX);
        String ten = String.valueOf(num/10);
        String one = String.valueOf(num%10);

        switch (ten){
            case "0":
                break;
            case "1":
                HRStenview.setImageResource(R.drawable.n_1);
                break;
            case "2":
                HRStenview.setImageResource(R.drawable.n_2);
                break;
            case "3":
                HRStenview.setImageResource(R.drawable.n_3);
                break;
            case "4":
                HRStenview.setImageResource(R.drawable.n_4);
                break;
            case "5":
                HRStenview.setImageResource(R.drawable.n_5);
                break;
            case "6":
                HRStenview.setImageResource(R.drawable.n_6);
                break;
            case "7":
                HRStenview.setImageResource(R.drawable.n_7);
                break;
            case "8":
                HRStenview.setImageResource(R.drawable.n_8);
                break;
            case "9":
                HRStenview.setImageResource(R.drawable.n_9);
                break;
        }

        switch (one){
            case "0":
                HRSoneview.setImageResource(R.drawable.n_0);
                break;
            case "1":
                HRSoneview.setImageResource(R.drawable.n_1);
                break;
            case "2":
                HRSoneview.setImageResource(R.drawable.n_2);
                break;
            case "3":
                HRSoneview.setImageResource(R.drawable.n_3);
                break;
            case "4":
                HRSoneview.setImageResource(R.drawable.n_4);
                break;
            case "5":
                HRSoneview.setImageResource(R.drawable.n_5);
                break;
            case "6":
                HRSoneview.setImageResource(R.drawable.n_6);
                break;
            case "7":
                HRSoneview.setImageResource(R.drawable.n_7);
                break;
            case "8":
                HRSoneview.setImageResource(R.drawable.n_8);
                break;
            case "9":
                HRSoneview.setImageResource(R.drawable.n_9);
                break;
        }
    }

    public static String returnService(String str) {
        int HRS, STEP, dist, cal;
        String hrs, step, vib;

        if (str.substring(4, 6).equals("03")) {
            //HRS
            HRS = Integer.parseInt(str.substring(12, 14), 16);
            hrs = Integer.toString(HRS);
            return hrs;
        } else if (str.substring(4, 6).equals("02")) {
            //pedometer
            STEP = Integer.parseInt(str.substring(10, 14), 16);
            step = Integer.toString(STEP);
            return step;
        }else if (str.substring(4, 6).equals("00")) {
            vib = "55050002050208EA";
            return vib;
        } else
            return "no service";

    }

    public static int returnCalorie(String str,String walk){
        // 이동거리(m) = ((키(cm)-100)*걸음수)/100
        // 마일당 칼로리(cal/mile) = 3.7103 + 0.2678*체중(kg) + (0.0359*(체중(kg)*60*0.006213)*2)*체중(kg)
        // 소비칼로리(cal) = 이동거리(m)*마일당 칼로리(cal/mile)*0.0006213
        String height, weight;
        height = getHexToDec(str.substring(12,16));
        weight = getHexToDec(str.substring(16,20));
        int Height = Integer.parseInt(height);
        int Weight = Integer.parseInt(weight);
        int pedo = Integer.parseInt(walk);
        int m = (Height-1000) * pedo  /100;
        double mile = 3.7103+0.2678*Weight+(0.0359*(Weight*60*0.006213)*2)*Weight;
        int Cal = (int) (m+mile*0.0006213);

        return Cal;
    }


    public static String getHexToDec(String hex) {
        long v = Long.parseLong(hex, 16);
        return String.valueOf(v);
    }

    private void service_init() {
        Intent bindIntent = new Intent(this, UartService.class);
        bindService(bindIntent, mServiceConnection, Context.BIND_AUTO_CREATE);
        LocalBroadcastManager.getInstance(this).registerReceiver(UARTStatusChangeReceiver, makeGattUpdateIntentFilter());
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(UartService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(UartService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(UartService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(UartService.DEVICE_DOES_NOT_SUPPORT_UART);
        return intentFilter;
    }
    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy()");

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(UARTStatusChangeReceiver);
        } catch (Exception ignore) {
            Log.e(TAG, ignore.toString());
        }
        unbindService(mServiceConnection);
        mService.stopSelf();
        mService= null;

    }

    @Override
    protected void onStop() {
        Log.d(TAG, "onStop");
        super.onStop();
    }

    @Override
    protected void onPause() {
        Log.d(TAG, "onPause");
        super.onPause();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        Log.d(TAG, "onRestart");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.d(TAG, "onResume");

        if (ContextCompat.checkSelfPermission(LinkingActivity.this,
                Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED
                &&
                ContextCompat.checkSelfPermission(LinkingActivity.this,
                        Manifest.permission.ACCESS_COARSE_LOCATION)
                        != PackageManager.PERMISSION_GRANTED) {
            askForLocationPermissions();
        } else {
        }

        if (!mBtAdapter.isEnabled()) {
            Log.i(TAG, "onResume - BT not enabled yet");
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        }

    }

    private void askForLocationPermissions() {
        if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.ACCESS_FINE_LOCATION)) {

            new android.support.v7.app.AlertDialog.Builder(this)
                    .setTitle("Location permessions needed")
                    .setMessage("you need to allow this permission!")
                    .setPositiveButton("Sure", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            ActivityCompat.requestPermissions(LinkingActivity.this,
                                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                                    LOCATION_PERMISSION_REQUEST_CODE);
                        }
                    })
                    .setNegativeButton("Not now", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .show();

        } else {

            // No explanation needed, we can request the permission.
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    LOCATION_PERMISSION_REQUEST_CODE);

        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE:
                if (isPermissionGranted(permissions, grantResults, Manifest.permission.ACCESS_FINE_LOCATION)) {
                    //Do you work
                } else {
                    Toast.makeText(this, "Can not proceed! i need permission" , Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    public static boolean isPermissionGranted(@NonNull String[] grantPermissions, @NonNull int[] grantResults,
                                              @NonNull String permission) {
        for (int i = 0; i < grantPermissions.length; i++) {
            if (permission.equals(grantPermissions[i])) {
                return grantResults[i] == PackageManager.PERMISSION_GRANTED;
            }
        }
        return false;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {

            case REQUEST_SELECT_DEVICE:
                if (resultCode == Activity.RESULT_OK && data != null) {
                    String deviceAddress = data.getStringExtra(BluetoothDevice.EXTRA_DEVICE);
                    mDevice = BluetoothAdapter.getDefaultAdapter().getRemoteDevice(deviceAddress);

                    Log.d(TAG, "... onActivityResultdevice.address==" + mDevice + "mserviceValue" + mService);
                    mService.connect(deviceAddress);
                }
                break;
            case REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    Toast.makeText(this, "Bluetooth has turned on ", Toast.LENGTH_SHORT).show();

                } else {
                    Log.d(TAG, "BT not enabled");
                    Toast.makeText(this, "Problem in BT Turning ON ", Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
            default:
                Log.e(TAG, "wrong request code");
                break;
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {

    }


    private void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();

    }

    @Override
    public void onBackPressed() {
        Intent backToMain = new Intent(LinkingActivity.this, MainActivity.class);
        startActivity(backToMain);
        this.finish();
    }
}
