package com.developertools.andronerds.onboarddiagnosticdeviceemulator;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Set;


public class MainActivity extends ActionBarActivity implements Button.OnClickListener{

    Set<BluetoothDevice> pairedDevices;
    ArrayAdapter<String> allDevices;
    ArrayList<String> pairedDevicesList;
    ArrayList<String> allDevicesList;
    Boolean flag;
    ListView listView;
    Button refreshButton;
    Button discoverableButton;
    Button serverButton;
    private static Button accelButt;
    private static Button breakButt;
    private static Button turnButt;
    private static Button fiveButt;
    private static Button tenButt;
    private static Button fifteenButt;
    private static Button crashButt;
    private static Button poiButt;
    private static Button lowGasButt;
    private static Button carOne;
    private static Button carTwo;
    private static Button carThree;
    private static JSONObject currentCar;
    IntentFilter filter;
    BroadcastReceiver receiver;
    String address;
    private static String secureAddress;
    private static boolean Driving = false;
    private static BluetoothAdapter adapt;
    private static Boolean pitchingState = false;
    static TextView s_Server;
    public static final Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d("HANDLER","recieved");
            Bundle b = msg.getData();
            Log.d("MainThread bundle:", b.toString());
            switch(b.getString("Purpose")){
                case "serverStatus":
                    s_Server.setText(b.getString("Text"));
                    break;
                case "HANDSHAKE":
                    secureAddress = b.getString("From");
                    connect(secureAddress);
                    JSONObject handShake = new JSONObject();
                    try{
                        handShake.put("Purpose","HANDSHAKE");
                        handShake.put("From",adapt.getAddress());
                    }catch(JSONException e){}
                    writeMessage(handShake);
                    break;
                case "DRIVEMODEON":
                    Driving = true;
                    setDriveButtons(true);
                    s_Server.setText("Driving");
                    Log.d("MainActivity", "Recieved Drive Mode");
                    break;
                case "DRIVEMODEOFF":
                    Driving = false;
                    setDriveButtons(false);
                    s_Server.setText("Server open");
                    //new AcceptThread().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
                    break;
                case "CAR":
                    writeMessage(currentCar);
                    break;
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        TextView serverstat = (TextView)findViewById(R.id.serverStatus);
        s_Server = serverstat;
        adapt = BluetoothAdapter.getDefaultAdapter();

        flag = false;
        allDevices = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,0);
        //
        refreshButton = (Button)findViewById(R.id.refresh);
        refreshButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                refresh();
            }
        });
        //
        discoverableButton = (Button)findViewById(R.id.discover);
        discoverableButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                if(adapt.getScanMode() != BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE){
                    Intent discoverIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,0);
                    startActivity(discoverIntent);
                }else{
                    Intent discoverIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    discoverIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,1);
                    startActivity(discoverIntent);
                }
            }
        });
        //
        serverButton = (Button)findViewById(R.id.server);
        serverButton.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Log.d("Server Button: ", "Pressed");;
                    new AcceptThread().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
            }
        });
        //
        carOne = (Button)findViewById(R.id.carOne);
        carOne.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Log.d("Car One ", "Pressed");
                setCar(1);
            }
        });
        //
        carTwo = (Button)findViewById(R.id.carTwo);
        carTwo.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Log.d("Car Two ", "Pressed");
                setCar(2);
            }
        });
        //
        carThree = (Button)findViewById(R.id.carThree);
        carThree.setOnClickListener(new View.OnClickListener(){
            public void onClick(View v){
                Log.d("Car Three ", "Pressed");;
                setCar(3);
            }
        });
        //
        breakButt = (Button)findViewById(R.id.breakButton);
        breakButt.setOnClickListener(this);
        //
        accelButt = (Button)findViewById(R.id.accelButton);
        accelButt.setOnClickListener(this);
        //
        turnButt = (Button)findViewById(R.id.turnButton);
        turnButt.setOnClickListener(this);
        //
        fiveButt = (Button)findViewById(R.id.fiveButton);
        fiveButt.setOnClickListener(this);
        //
        tenButt = (Button)findViewById(R.id.tenButton);
        tenButt.setOnClickListener(this);
        //
        fifteenButt = (Button)findViewById(R.id.fifteenButton);
        fifteenButt.setOnClickListener(this);
        //
        crashButt = (Button)findViewById(R.id.crashButton);
        crashButt.setOnClickListener(this);
        //
        poiButt = (Button)findViewById(R.id.poiButton);
        poiButt.setOnClickListener(this);
        //
        lowGasButt = (Button)findViewById(R.id.lowGasButton);
        lowGasButt.setOnClickListener(this);

        setDriveButtons(false);

        pairedDevicesList = new ArrayList<>();
        allDevicesList = new ArrayList<>();
        address = "";

        checkBt();

        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();

                if(BluetoothDevice.ACTION_FOUND.equals(action)){
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    String s = "";

                    for(int i = 0; i < pairedDevicesList.size(); i++){
                        if(device.getAddress().equals(pairedDevicesList.get(i))){
                            s = "PAIRED";
                        }else{
                            s = "";
                        }
                    }
                    for(int i = 0; i < allDevicesList.size(); i++){
                        if(device.getAddress().equals(allDevicesList.get(i))){
                            return;
                        }
                    }

                    allDevices.add(device.getName() + " " + s + "\n" + device.getAddress());
                    allDevices.notifyDataSetChanged();
                    allDevicesList.add(device.getAddress());

                }else if(BluetoothAdapter.ACTION_DISCOVERY_STARTED.equals(action)){

                }else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)){

                }else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)){
                    if(adapt.getState() == adapt.STATE_OFF){
                        enableBt();
                    }
                }
            }
        };
        setCar(1);
        refresh();
    }

    private static void setCar(int CAR){
        currentCar = new JSONObject();
        try{
            currentCar.put("Purpose","CAR");
            switch (CAR){
                case 1:
                    currentCar.put("Year", "1992");
                    currentCar.put("Make","Nissan Sentra");
                    currentCar.put("Model","GXE");
                    currentCar.put("Vin","JN1EB31P6NU133958");
                    currentCar.put("Color","#969696");
                    carOne.setEnabled(false);
                    carTwo.setEnabled(true);
                    carThree.setEnabled(true);
                    break;
                case 2:
                    currentCar.put("Year", "2003");
                    currentCar.put("Make","Kia");
                    currentCar.put("Model","Sportage");
                    currentCar.put("Vin","9D7FS2FHQ5DI7G5D5");
                    currentCar.put("Color","#000000");
                    carOne.setEnabled(true);
                    carTwo.setEnabled(false);
                    carThree.setEnabled(true);
                    break;
                case 3:
                    currentCar.put("Year", "1927");
                    currentCar.put("Make","Nsan Sentra");
                    currentCar.put("Model","GE");
                    currentCar.put("Vin","DH698ED9HP7Z11GD6");
                    currentCar.put("Color","#969696");
                    carOne.setEnabled(true);
                    carTwo.setEnabled(true);
                    carThree.setEnabled(false);
                    break;
            }
        }catch(JSONException e){ e.printStackTrace();}
    }

    public static JSONObject getCurrentCar(){return currentCar;}

    public static void setDriveButtons(boolean state){
        breakButt.setEnabled(state);
        accelButt.setEnabled(state);
        turnButt.setEnabled(state);
        fiveButt.setEnabled(state);
        tenButt.setEnabled(state);
        fifteenButt.setEnabled(state);
        crashButt.setEnabled(state);
        poiButt.setEnabled(state);
        lowGasButt.setEnabled(state);
    }

    public static Boolean isPitching(){
        return pitchingState;
    }

    public static void setPitchingState(Boolean x){
        pitchingState = x;
    }

    public static boolean isDriving(){
        return Driving;
    }

    public static void writeMessage(JSONObject job){

        Message msg = Message.obtain();
        Bundle b = new Bundle();
        b.putString("job",job.toString());
        msg.setData(b);
        Log.d("Sending", b.toString());
        SendThread.mHandler.sendMessage(msg);
    }

    public static void connect(String address){
        JSONObject job = new JSONObject();
        try {
            job.put("To", address);
            job.put("From",adapt.getAddress());
            pitchingState = true;
            Log.d("Connecting with", job.toString());

            new SendThread().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR,job);

        }catch(JSONException e){}


    }

    public void refresh(){
        allDevices.clear();
        allDevices.notifyDataSetChanged();
        allDevicesList.clear();
        pairedDevicesList.clear();
        registerReceivers();
        getPairedDevices();
        startDiscovery();
    }

    public void registerReceivers(){
        allDevices.notifyDataSetChanged();
        filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        registerReceiver(receiver,filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_STARTED);
        registerReceiver(receiver,filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        registerReceiver(receiver,filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        registerReceiver(receiver,filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        try{
            unregisterReceiver(receiver);
        }catch(IllegalArgumentException e) {

        }
    }

    public void getPairedDevices(){
        pairedDevices = adapt.getBondedDevices();
        if(pairedDevices.size() > 0){
            for(BluetoothDevice device : pairedDevices){
                pairedDevicesList.add(device.getAddress());
            }
        }
    }

    public void startDiscovery(){
        adapt.cancelDiscovery();
        adapt.startDiscovery();
    }

    public void checkBt(){

        if(adapt == null){
            Toast.makeText(getApplicationContext(), "Bluetooth Is Not Supported", Toast.LENGTH_LONG).show();
            finish();
        }else{
            if(!adapt.isEnabled()){
                enableBt();
                refresh();
            }
        }
    }

    public void enableBt(){
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
        startActivityForResult(enableBtIntent, 1);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(resultCode == RESULT_CANCELED){
            Toast.makeText(getApplicationContext(), "Sorry bro, need bluetooth", Toast.LENGTH_LONG).show();
            finish();
        }if(resultCode == RESULT_OK){
            //getPairedDevices();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onClick(View v) {
        JSONObject job = new JSONObject();
        try{
            job.put("Purpose","Driving");
            switch (v.getId()){
                case R.id.accelButton:
                    job.put("Event","accel");
                    break;
                case R.id.breakButton:
                    job.put("Event","break");
                    break;
                case R.id.turnButton:
                    job.put("Event","turn");
                    break;
                case R.id.fiveButton:
                    job.put("Event","five");
                    break;
                case R.id.tenButton:
                    job.put("Event","ten");
                    break;
                case R.id.fifteenButton:
                    job.put("Event","fifteen");
                    break;
                case R.id.crashButton:
                    job.put("Event","crash");
                    break;
                case R.id.poiButton:
                    job.put("Event","poi");
                    break;
                case R.id.lowGasButton:
                    job.put("Event","lowGas");
                    break;
            }
            Log.d("Event JSON:", job.toString());
            writeMessage(job);
        }catch(JSONException e){

        }
    }
}