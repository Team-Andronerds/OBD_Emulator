package com.developertools.andronerds.onboarddiagnosticdeviceemulator;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Set;


public class MainActivity extends ActionBarActivity implements AdapterView.OnItemClickListener{

    Set<BluetoothDevice> pairedDevices;
    ArrayAdapter<String> allDevices;
    ArrayList<String> pairedDevicesList;
    ArrayList<String> allDevicesList;
    Boolean flag;
    ListView listView;
    Button refreshButton;
    Button discoverableButton;
    Button serverButton;
    IntentFilter filter;
    BroadcastReceiver receiver;
    String address;
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
                    connect(b.getString("From"));
                    break;
                case "Reopen":
                    new AcceptThread().execute();
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
                    new AcceptThread().execute();
            }
        });
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
        refresh();
    }

    public static Boolean isPitching(){
        return pitchingState;
    }

    public static void setPitchingState(Boolean x){
        pitchingState = x;
    }

    public static void sendMessage(JSONObject job){

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

            JSONObject handShake = new JSONObject();
            try{
                handShake.put("Purpose","HANDSHAKE");
                handShake.put("From",adapt.getAddress());
            }catch(JSONException e){}
            sendMessage(handShake);

            new SendThread().execute(job);

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
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {

        if(adapt.isDiscovering()){
            adapt.cancelDiscovery();
        }
        if(allDevices.getItem(position).contains("PAIRED")){
            if(!address.equals("")){
                listView.setItemChecked(position, false);
                view.setBackgroundColor(Color.TRANSPARENT);

                address = "";
            }else{
                listView.setItemChecked(position, true);
                view.setBackgroundColor(Color.CYAN);

                if(pairedDevices.size() > 0){
                    for(BluetoothDevice device : pairedDevices){
                        if(allDevices.getItem(position).contains(device.getAddress())){
                            address = device.getAddress();
                        }
                    }
                }

            }
        }else{
            Toast.makeText(getApplicationContext(), "device is not paired", Toast.LENGTH_SHORT).show();
        }
    }
}