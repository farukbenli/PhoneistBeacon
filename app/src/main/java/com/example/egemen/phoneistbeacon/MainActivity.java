package com.example.egemen.phoneistbeacon;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.RemoteException;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import org.altbeacon.beacon.Beacon;
import org.altbeacon.beacon.BeaconConsumer;
import org.altbeacon.beacon.BeaconManager;
import org.altbeacon.beacon.BeaconParser;
import org.altbeacon.beacon.Identifier;
import org.altbeacon.beacon.MonitorNotifier;
import org.altbeacon.beacon.RangeNotifier;
import org.altbeacon.beacon.Region;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;

public class MainActivity extends AppCompatActivity implements BeaconConsumer {

    // BEACON
    private static final String myUUID = "F3D1D52B-6EB0-FDAF-B51C-1ADE24648C14"; // phoneist UUID
    public static final String TAG = "BizimBeacon";
    private BeaconManager beaconManager;
    // BEACON

    private String macAddress = null;
    //private String androidId;
    private String firstTime = null, lastTime = null;
    private String date = null;
    private Calendar cal = null;
    private WifiInfo wInfo = null;
    private WifiManager wifiManager = null;
    private Identifier major = null, minor = null;
    private String IP = "192.168.2.143";
    private long scanPeriod = 10000l;   // 10 saniye
    private boolean secondQueryControl = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        try {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.activity_main);

            verifyBluetooth();

            // BEACON
            beaconManager = BeaconManager.getInstanceForApplication(this);
            beaconManager.getBeaconParsers().add(new BeaconParser()
                    .setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24,d:25-25"));
            beaconManager.bind(this);
            // BEACON

            //beaconManager.setBackgroundMode(true);
            //beaconManager.setBackgroundScanPeriod(scanPeriod);
            //beaconManager.updateScanPeriods();

            //androidId = Settings.Secure.getString(this.getContentResolver(), Settings.Secure.ANDROID_ID);

        } catch (Exception ex) {
            Log.e("onCreate", ex.toString());
        }
    }

    class SendDataTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... params) {
            if (params[0].equals("first")) {
                firstSend();
            }
            else if (params[0].equals("second")){
                secondSend();
            }
            return null;
        }

        public void firstSend() {
            try {
                // url where the data will be posted
                String postReceiverUrl = "http://" + IP + "/yoklama_sistemi/receive_data.php";
                Log.e(TAG, "post first URL: " + postReceiverUrl);

                // HttpClient
                HttpClient httpClient = new DefaultHttpClient();

                // post header
                HttpPost httpPost = new HttpPost(postReceiverUrl);

                // add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(5);
                nameValuePairs.add(new BasicNameValuePair("firsttime", firstTime));
                nameValuePairs.add(new BasicNameValuePair("date", date));
                nameValuePairs.add(new BasicNameValuePair("major", major.toString()));
                nameValuePairs.add(new BasicNameValuePair("minor", minor.toString()));
                nameValuePairs.add(new BasicNameValuePair("macaddress", macAddress));

                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // execute HTTP post request
                HttpResponse response = httpClient.execute(httpPost);
                HttpEntity resEntity = response.getEntity();

                if (resEntity != null) {

                    String responseStr = EntityUtils.toString(resEntity).trim();
                    Log.e(TAG, "First Response: " + responseStr);

                    secondQueryControl = true;
                    // you can add an if statement here and do other actions based on the response
                }

            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void secondSend() {
            try {
                // url where the data will be posted
                String postReceiverUrl = "http://" + IP + "/yoklama_sistemi/update_data.php";
                Log.e(TAG, "post second URL: " + postReceiverUrl);

                // HttpClient
                HttpClient httpClient = new DefaultHttpClient();

                // post header
                HttpPost httpPost = new HttpPost(postReceiverUrl);

                // add your data
                List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(6);
                nameValuePairs.add(new BasicNameValuePair("firsttime", firstTime));
                nameValuePairs.add(new BasicNameValuePair("date", date));
                nameValuePairs.add(new BasicNameValuePair("major", major.toString()));
                nameValuePairs.add(new BasicNameValuePair("minor", minor.toString()));
                nameValuePairs.add(new BasicNameValuePair("macaddress", macAddress));
                nameValuePairs.add(new BasicNameValuePair("lasttime", lastTime));

                httpPost.setEntity(new UrlEncodedFormEntity(nameValuePairs));

                // execute HTTP post request
                HttpResponse response = httpClient.execute(httpPost);
                HttpEntity resEntity = response.getEntity();

                if (resEntity != null) {

                    String responseStr = EntityUtils.toString(resEntity).trim();
                    Log.e(TAG, "Second Response: " + responseStr);

                    // you can add an if statement here and do other actions based on the response
                }
            } catch (ClientProtocolException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    //BEACON
    @Override
    public void onBeaconServiceConnect() {
        //final Region region = new Region("myBeaons", Identifier.parse(myUUID), null, null);
        final Region region = new Region("myBeacons", null, null, null);

        beaconManager.setMonitorNotifier(new MonitorNotifier() {
            @Override
            public void didEnterRegion(Region region) {
                try {
                    Log.e(TAG, "didEnterRegion");
                    beaconManager.startRangingBeaconsInRegion(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void didExitRegion(Region region) {
                try {
                    Log.e(TAG, "didExitRegion");
                    beaconManager.stopRangingBeaconsInRegion(region);
                } catch (RemoteException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void didDetermineStateForRegion(int i, Region region) {

            }
        });

        beaconManager.setRangeNotifier(new RangeNotifier() {
            @Override
            public void didRangeBeaconsInRegion(Collection<Beacon> beacons, Region region) {
                /*
                for (Beacon oneBeacon : beacons) {
                    Log.e(TAG, "distance: " + oneBeacon.getDistance() + " id:" + oneBeacon.getId1() + "/" + oneBeacon.getId2() + "/" + oneBeacon.getId3());

                    if (major == null && minor == null) {
                        try {
                            major = oneBeacon.getId2();
                            minor = oneBeacon.getId3();

                            beaconManager.setForegroundScanPeriod(scanPeriod);
                            try {
                                beaconManager.updateScanPeriods();
                            } catch (RemoteException e) {
                                e.printStackTrace();
                            }

                            //wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                            //wInfo = wifiManager.getConnectionInfo();
                            //macAddress = wInfo.getMacAddress();

                            macAddress = "mac";

                            cal = Calendar.getInstance();

                            SimpleDateFormat sdfForFirstTime = new SimpleDateFormat("HH:mm:ss");
                            firstTime = sdfForFirstTime.format(cal.getTime());

                            SimpleDateFormat sdfForDate = new SimpleDateFormat("yyyy-MM-dd");
                            date = sdfForDate.format(cal.getTime());

                            new SendDataTask().execute("first");
                        } catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }
                    else if (major != null && minor != null && secondQueryControl == true) {
                        try {
                            cal = Calendar.getInstance();

                            SimpleDateFormat sdfForLastTime = new SimpleDateFormat("HH:mm:ss");
                            lastTime = sdfForLastTime.format(cal.getTime());

                            new SendDataTask().execute("second");
                        }
                        catch (Exception ex) {
                            ex.printStackTrace();
                        }
                    }

                }
                */

                if (major == null && minor == null) {
                    try {
                        beaconManager.setForegroundScanPeriod(scanPeriod);
                        try {
                            beaconManager.updateScanPeriods();
                        } catch (RemoteException e) {
                            e.printStackTrace();
                        }
                        major = beacons.iterator().next().getId2();
                        minor = beacons.iterator().next().getId3();

                        wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
                        wInfo = wifiManager.getConnectionInfo();
                        macAddress = wInfo.getMacAddress();

                        cal = Calendar.getInstance();

                        SimpleDateFormat sdfForFirstTime = new SimpleDateFormat("HH:mm:ss");
                        firstTime = sdfForFirstTime.format(cal.getTime());

                        SimpleDateFormat sdfForDate = new SimpleDateFormat("yyyy-MM-dd");
                        date = sdfForDate.format(cal.getTime());

                        new SendDataTask().execute("first");
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                else if (secondQueryControl == true) {
                    try {
                        cal = Calendar.getInstance();

                        SimpleDateFormat sdfForLastTime = new SimpleDateFormat("HH:mm:ss");
                        lastTime = sdfForLastTime.format(cal.getTime());

                        new SendDataTask().execute("second");
                    }
                    catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
            }
        });

        try {
            beaconManager.startMonitoringBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
    // BEACON

    private void verifyBluetooth() {
        try {
            // dialogClickListener değişkeni AlertDialog.builder da sorulan soruya Evet veya Hayır denmesi durumunda neler yapılacağını tanımlar.
            DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    switch (which) {
                        case DialogInterface.BUTTON_POSITIVE:
                            Intent enableBluetooth = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                            startActivity(enableBluetooth);
                            break;
                        case DialogInterface.BUTTON_NEGATIVE:
                            finish();
                            System.exit(0);
                            break;
                    }
                }
            };

            if (!BeaconManager.getInstanceForApplication(this).checkAvailability()) {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Bluetooth not enabled");
                builder.setMessage("Do you want to enable Bluetooth?");
                builder.setPositiveButton(android.R.string.yes, dialogClickListener);
                builder.setNegativeButton(android.R.string.no, dialogClickListener);
                builder.show();
            }
        } catch (RuntimeException e) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("Bluetooth LE not available");
            builder.setMessage("Sorry, this device does not support Bluetooth LE.");
            builder.setPositiveButton(android.R.string.ok, null);
            builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
                @Override
                public void onDismiss(DialogInterface dialog) {
                    finish();
                    System.exit(0);
                }
            });
            builder.show();
        }
    }

    @Override
    protected void onStop() {
        try {
            final Region region = new Region("myBeacons", null, null, null);
            beaconManager.startMonitoringBeaconsInRegion(region);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        //beaconManager.setBackgroundMode(true);
        super.onStop();
    }

    @Override
    protected void onRestart() {
        //beaconManager.setBackgroundMode(false);
        super.onRestart();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        beaconManager.unbind(this); // BEACON
    }
}
