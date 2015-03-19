package com.developertools.andronerds.onboarddiagnosticdeviceemulator;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.UUID;

/**
 * Created by Dilancuan on 3/18/2015.
 */
public class SendThread extends AsyncTask<JSONObject, Integer, Void> {
    private BluetoothDevice catcher;
    private BluetoothSocket catcherSocket;
    private BluetoothAdapter pitcher;
    private static boolean waitingForMessage = true;
    private static JSONObject ball;
    public static final Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.d("Msg", "recieved");
            Bundle b = msg.getData();

            try{
                ball = new JSONObject(b.getString("job"));
                Log.d("sender handler", ball.toString());
            }catch(JSONException e){}

            waitingForMessage= false;

        }
    };
    @Override
    protected Void doInBackground(JSONObject... Params){
        JSONObject job = Params[0];
        pitcher = BluetoothAdapter.getDefaultAdapter();
        BluetoothSocket tmp;
        Log.d("trying", "parse");
        try {
            final String toWhom = job.getString("To");
            Log.d("PITCHING TO", toWhom);

            for(BluetoothDevice device: pitcher.getBondedDevices()){
                if(device.getAddress().equals(toWhom)){
                    catcher = device;
                    Log.d("Found", device.toString());
                }
            }
        }catch(JSONException e){
            Log.d("parse", "failed");
        }

        // Get a BluetoothSocket to connect with the given BluetoothDevice
            do{
            while(waitingForMessage){};
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                tmp = catcher.createInsecureRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"));
                catcherSocket = tmp;
            } catch (IOException e) {
                Log.d("Run: ", "mmDevice failed");
            }


            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                Log.d("Run: ", "Connecting");
                catcherSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                try {
                    Log.d("Run: ", " Connecting failed/Closing started");
                    catcherSocket.close();
                } catch (IOException closeException) {
                    Log.d("Run: ", "Closing failed");
                }
            }

            manageConnectedSocket();
            try{
                catcherSocket.close();
            }catch(IOException e){e.printStackTrace();}

        }while(MainActivity.isPitching());
        return null;
    }

    private void manageConnectedSocket(){
        Log.d("isPitching: ", "Waiting");
        final OutputStream mmOutputStream;
        OutputStream tmpOut = null;

        try{
            Log.d("manager: ", "fetching output");
            tmpOut = new BufferedOutputStream(catcherSocket.getOutputStream());
        }catch (IOException e){
            Log.d("manager: ", "fetching failed");
        }

        mmOutputStream = tmpOut;

        try{
            if (mmOutputStream != null) {
                Log.d("manager: ", "Writing bytes of " + ball.toString());

                mmOutputStream.write(ball.toString().getBytes());
                mmOutputStream.flush();
                Log.d("manager: ","Writing successful");
            }
        }catch(IOException e){
            Log.d("manager: ", "Writing failed");
        }

        try{
            if(ball.getString("Purpose").equals("HANDSHAKE")){
                MainActivity.setPitchingState(false);
            }
        }catch(JSONException e){
            e.printStackTrace();
        }

        waitingForMessage = true;
    }

    @Override
    protected void onPostExecute(Void aVoid) {
        super.onPostExecute(aVoid);
        MainActivity.setPitchingState(false);
        Log.d("Pitching","Finished");

        Message msg = Message.obtain();
        Bundle b = new Bundle();
        b.putString("Purpose","serverStatus");
        b.putString("Text","Server Open");
        msg.setData(b);
        MainActivity.mHandler.sendMessage(msg);

        msg = Message.obtain();
        b = new Bundle();
        b.putString("Purpose","Reopen");
        msg.setData(b);
        MainActivity.mHandler.sendMessage(msg);
    }
}
