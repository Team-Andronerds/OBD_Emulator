package com.developertools.andronerds.onboarddiagnosticdeviceemulator;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.UUID;

public class AcceptThread extends AsyncTask {
    public static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    private BluetoothServerSocket mmServerSocket;
    private static boolean serving = true;
    private static Message msg1;
    private static Bundle b;
    public static final int MESSAGE_READ = 1;
    public static final Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch(msg.what){
                case MESSAGE_READ:
                    byte[] readbuf = (byte[]) msg.obj;
                    String readmsg = new String(readbuf);
                    JSONObject job;
                    Log.d("Message is: ", "" + readmsg);
                    try {
                        job = new JSONObject(readmsg);
                        //Log.d("JSON is: ", job.toString());
                        Log.d("Purpose is:", job.getString("Purpose"));
                        switch(job.getString("Purpose")){
                            case "HANDSHAKE":
                                Log.d("case", "HANDSHAKE");
                                msg1 = Message.obtain();
                                b = new Bundle();
                                b.putString("Purpose","HANDSHAKE");
                                b.putString("From",job.getString("From"));
                                msg1.setData(b);
                                MainActivity.mHandler.sendMessage(msg1);
                                break;
                            case "DISCONNECT":
                                serving = false;
                                MainActivity.setPitchingState(false);
                                break;
                            case "DRIVEMODEON":
                                Log.d("Case","DRIVEMODEON");
                                msg1 = Message.obtain();
                                b = new Bundle();
                                b.putString("Purpose","DRIVEMODEON");
                                msg1.setData(b);
                                MainActivity.mHandler.sendMessage(msg1);
                                break;
                            case "DRIVEMODEOFF":
                                Log.d("Case","DRIVEMODEOFF");
                                msg1 = Message.obtain();
                                b = new Bundle();
                                b.putString("Purpose","DRIVEMODEOFF");
                                msg1.setData(b);
                                MainActivity.mHandler.sendMessage(msg1);
                                break;
                            case "CAR":
                                Log.d("Case","CAR REQUESTED");
                                msg1 = Message.obtain();
                                b = new Bundle();
                                b.putString("Purpose","CAR");
                                msg1.setData(b);
                                MainActivity.mHandler.sendMessage(msg1);
                                break;
                        }
                    }catch(JSONException e){
                        Log.d("FAILED", e.toString());
                        e.printStackTrace();
                    }
            }
        }
    };

    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        // Use a temporary object that is later assigned to mmServerSocket,
        // because mmServerSocket is final
        BluetoothServerSocket tmp = null;
        try {
            // MY_UUID is the app's UUID string, also used by the client code
            tmp = BluetoothAdapter.getDefaultAdapter().listenUsingInsecureRfcommWithServiceRecord("OBD", MY_UUID);
        } catch (IOException e) {
            Log.d("AcceptThread:", "FAILED");
        }
        mmServerSocket = tmp;
        serving = true;
    }

    /** Will cancel the listening socket, and cause the thread to finish */
    public void cancel() {
        try {
            mmServerSocket.close();
        } catch (IOException e) { }
    }

    @Override
    protected Object doInBackground(Object[] params) {

        BluetoothSocket socket = null;
        byte[] buffer = new byte[1024];
        int bytes;

        // Keep listening until exception occurs or a socket is returned
        while(mmServerSocket==null);
        Message msg = Message.obtain();
        Bundle b = new Bundle();
        b.putString("Purpose","serverStatus");
        b.putString("Text","Server Open");
        msg.setData(b);
        MainActivity.mHandler.sendMessage(msg);
        while(serving) {

            try {
                Log.d("Run", "trying accept");
                if (serving) {
                    socket = mmServerSocket.accept();
                    Log.d("Run:", "mmSeverSocket accept SUCCESS");
                }else{
                    try {
                        mmServerSocket.close();
                        Log.d("Run:", "mmSeverSocket pitching, waiting...");
                    } catch (IOException e) {}
                }
            } catch (IOException e) {
                Log.d("Run:", "mmSeverSocket accept FAIL");

            }

            // If a connection was accepted
            if (socket != null) {
                // Do work to manage the connection (in a separate thread)
                try {
                    bytes = socket.getInputStream().read(buffer);
                    mHandler.obtainMessage(MESSAGE_READ, bytes, -1, buffer).sendToTarget();
                } catch (IOException e) {
                    Log.d("Run", "Socket Empty");
                }

            }
        }
        try {
            mmServerSocket.close();
        } catch (IOException e) {}

        if(MainActivity.isDriving()){
            Log.d("Driving engaged","Closing listener");
            return null;
        }
        Log.d("Disconnecting","...");
        msg = Message.obtain();
        b = new Bundle();
        b.putString("Purpose","serverStatus");
        if(MainActivity.isPitching()){
            b.putString("Text","Server Writing");
        }else{
            b.putString("Text","Server Closed");
        }
        msg.setData(b);
        MainActivity.mHandler.sendMessage(msg);
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        Log.d("AcceptThread","CLOSED");
    }
}
