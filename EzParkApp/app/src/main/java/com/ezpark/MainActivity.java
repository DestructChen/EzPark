package com.ezpark;

//android stuff

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;

//bluetooth
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;

//android layout
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

//datatypes
import java.util.ArrayList;
import java.util.UUID;

//exception types
import java.io.IOException;


public class MainActivity extends Activity {


    // Final Set variables
    //UUID [Universally unique identifier) the one used is common for (SPP) Serial Port Profile
    //numdevices refers to the number of arduino modules
    //MAClist is an array of the MAC addresses of the arduino modules
    private static UUID MY_UUID;
    private static int numdevices;
    private static String[] MAClist;


    public MyApp app;
    private BluetoothAdapter BA;
    public static String address; //address refers to the current MAC address of the connected module
    ToggleButton bttoggle; //bluetooth enable toggle

    Spinner devicelist; //dropdown of MAClist

    private ProgressDialog progress;
    public BluetoothSocket btSocket = null;
    public BluetoothDevice btDevice;
    boolean isBtConnected = false;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        boolean ignoreBTforemulator = false; //for purposes of testing with android studio emulator which does not support Bluetooth

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BA = BluetoothAdapter.getDefaultAdapter();
        bttoggle = (ToggleButton) findViewById(R.id.toggleButton);
        devicelist = (Spinner) findViewById(R.id.spinner);

        app = (MyApp) getApplication();
        MAClist = app.getMAClist();
        numdevices = app.getNumdevices();
        MY_UUID = app.getMyUuid();

        if (ignoreBTforemulator == true) {

        } else if (BA == null) { //if the android device does not have a bluetooth adapater, then show an error page
            Toast.makeText(getApplicationContext(), "Bluetooth Adapter not found", Toast.LENGTH_LONG).show();
            setContentView(R.layout.nobluetooth);
        } else {

            //for purposes of this project, we only have one.
            //initial check, if bluetooth is on, toggle button  is on and also select device list enabled
            //create list of arduino devices
            try {
                if (BA.isEnabled()) {
                    bttoggle.setChecked(true);
                    devicelist.getSelectedView().setEnabled(true);
                    devicelist.setEnabled(true);
                } else {
                    bttoggle.setChecked(false);
                    devicelist.getSelectedView().setEnabled(false);
                    devicelist.setEnabled(false);
                }
            } catch (NullPointerException e) {

            }
            list();
        }


        //toggle button for bluetooth enable
        bttoggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    Intent getVisible = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
                    startActivityForResult(getVisible, 0);
                    devicelist.getSelectedView().setEnabled(true);
                    devicelist.setEnabled(true);
                    Toast.makeText(getApplicationContext(), "Turned on", Toast.LENGTH_SHORT).show();
                } else {
                    devicelist.getSelectedView().setEnabled(false);
                    devicelist.setEnabled(false);
                    BA.disable();
                }
            }
        });

    }

    //returns list of already defined Arduino modules
    //check MyApp to set variables
    //edit MACList && numdevices to add more
    public void list() {
        ArrayList<String> list = new ArrayList<String>();
        list.add(0, "Connect to arduino"); //default text
        for (int i = 1; i < numdevices + 1; i++) {
            list.add(i, "Module " + i + " " + MAClist[i - 1]);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, list);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        devicelist.setAdapter(adapter);
        devicelist.setOnItemSelectedListener(myListSelectedListener);
    }

    //spinner(aka dropdown) item selected function
    private AdapterView.OnItemSelectedListener myListSelectedListener = new AdapterView.OnItemSelectedListener() {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
            // Get the device MAC address, the last 17 chars
            //ensure bt is enabled, and the default item is not selected (ie. pos!=0)
            if (BA.isEnabled()) {
                if (position != 0) {
                    String info = ((TextView) view).getText().toString();
                    address = info.substring(info.length() - 17);


                        new ConnectBT().execute();

                }
            } else {
                devicelist.getSelectedView().setEnabled(false);
                devicelist.setEnabled(false);
                Toast.makeText(getApplicationContext(), "Bluetooth is not enabled", Toast.LENGTH_SHORT).show();
                BA.disable();
                bttoggle.setChecked(false);
            }
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {

        }
    };

    //Start Reservation button's Onclick will execute this function
    //check if connected to arduino first
    public void availableSpaces(View v) {
        if (!BA.isEnabled()) {
            msg("please turn on bluetooth");
        } else if (address == null || btSocket == null) {
            try {
                address = btSocket.getRemoteDevice().getAddress();
            } catch (NullPointerException e) {
                msg("please select an arduino to connect to");
            }
        } else if (address != null) {
            Intent i = new Intent(MainActivity.this, startReservation.class);
            startActivity(i);
        }
    }


    //End Reservation button's Onclick will execute this function
    //check if connected to arduino first
    public void payment(View v) {
        if (!BA.isEnabled()) {
            msg("please turn on bluetooth");
        } else if (address == null || btSocket == null) {
            try {
                address = btSocket.getRemoteDevice().getAddress();
            } catch (NullPointerException e) {
                msg("please select an arduingo to connect to");
            }
        } else if (address != null) {
            Intent i = new Intent(MainActivity.this, endReservation.class);
            startActivity(i);
        }

    }


    //parking rates button
    public void parkingRates(View v) {
        Intent i = new Intent(MainActivity.this, parkingrates.class);
        startActivity(i);
    }


    //Bluetooth Connection
    //while the progress dialog is shown, the connection is done in background
    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {

        @Override
        protected void onPreExecute() {
            progress = ProgressDialog.show(MainActivity.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) {
            try {
                if (app.getBtSocket()==null) {
                    BA = BluetoothAdapter.getDefaultAdapter();                              //get the mobile bluetooth device
                    btDevice = BA.getRemoteDevice(address);                                 //connects to the device's address and checks if it's available
                    btSocket = btDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID); //create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                    if (btSocket.isConnected()) {
                        app.setBTSocket(btSocket);                                          //set the global bluetooth socket connection
                    }

                } else {
                    BA = BluetoothAdapter.getDefaultAdapter();
                    btDevice = BA.getRemoteDevice(address);
                    btSocket = btDevice.createInsecureRfcommSocketToServiceRecord(MY_UUID);
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();
                    if (btSocket.isConnected()) {
                        app.setBTSocket(btSocket);                                          //set the global bluetooth socket connection
                    }
                }
            } catch (IOException e) {
                msg("That device may not be available");
            }

            return null;
        }

        @Override
        protected void onPostExecute(Void result) //after the doInBackground, display result msg
        {
            super.onPostExecute(result);

            if (btSocket.getRemoteDevice().getAddress().equals(address)) {
                msg("Connected.");
                isBtConnected=true;

            } else {
                msg("Connection Failed");
                isBtConnected=false;

            }
            progress.dismiss();


        }
    }


    //function that recieves string and outputs as a toast (a message on screen)
    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }

}

