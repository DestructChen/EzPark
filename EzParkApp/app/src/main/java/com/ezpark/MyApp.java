package com.ezpark;

import android.app.Application;
import android.bluetooth.BluetoothSocket;

import java.util.UUID;


/*
The purpose of this application is to manage global data across the different activities.
The main benefit of this, is so that the bluetooth connection can be sustained across different activities (i.e the different screens of the app)
*/

public class MyApp extends Application {
    BluetoothSocket btSocket = null;


    private static final double[] rateList = new double[]{
            8.00,
            14.00,
            18.00,
            25.00,
    };
    //NOTE:  changing rate tier values will require changing values in receiveDuration of payment.java
    private static final String[] ratetiers = new String[]{
            "0-2hrs",
            "2-4hrs",
            "4-6hrs",
            "6hrs+",
    };

    private static final String[] MAClist= new String[]{
            "00:6A:8E:16:C7:B1"

    };

    private static final UUID MY_UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static final int numdevices = MAClist.length;

    public int getNumdevices(){
        return numdevices;
    }

    public UUID getMyUuid(){
        return MY_UUID;
    }

    public String[] getMAClist() {
        return MAClist;
    }

    public double[] getRateList(){
        return rateList;
    }

    public String[] getRatetiers(){
        return ratetiers;
    }

    public BluetoothSocket getBtSocket(){
        return btSocket;
    }

    public void setBTSocket(BluetoothSocket Main){
        if(Main !=null) {
            this.btSocket = Main;
        }
    }



}



