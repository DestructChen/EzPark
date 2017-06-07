package com.ezpark;

import android.app.Activity;
import android.app.ProgressDialog;

import android.bluetooth.BluetoothSocket;

import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;


import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.StringTokenizer;


/**
 * Created by David on 30/05/2017.
 */


public class startReservation extends Activity {
    public InputStream in = null;
    public OutputStream out = null;
    public BluetoothSocket btSocket;
    private MyApp app;

    TextView receiveabc;
    String plateNumber;
    String command;
    int selectedposition;

    GridView gridView;
    ArrayAdapter<String> adapter;
    ArrayList<String> spacelist;
    ProgressDialog pd;


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.startreservation);
        app = (MyApp) getApplicationContext();
        try {
            btSocket = app.getBtSocket();
        } catch (Exception e) {
            msg(e.toString());
        }


        receiveabc = (TextView) findViewById(R.id.textreceive);

        gridView = (GridView) findViewById(R.id.gridview);
        spacelist = new ArrayList<String>();
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, spacelist);

        selectedposition = -1;
        gridView.setAdapter(adapter);
        new retrieveSpacesInfo().execute();
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                if ((gridView.getItemAtPosition(position).toString().contains("vacant"))) {
                    msg(position + " has been selected");
                    selectedposition = position;
                } else {

                    msg(position + " is not available");
                }
            }
        });


    }

    public void requestSpaces() {

        if (btSocket != null) {
            try {
                out = btSocket.getOutputStream();

                command = "plateNone" + " getSpaces";
                out.flush();
                out.write(command.getBytes());


            } catch (IOException e) {
                msg(e.toString());
            } catch (Exception e) {
                msg(e.toString());
            }
        }


    }


    public void receiveSpaces() {
        try {
            in = btSocket.getInputStream();
            DataInputStream mmInStream = new DataInputStream(in);
            byte[] buffer = new byte[256];
            if (mmInStream.available() > 0) {


                int numbytes = mmInStream.read(buffer);

                String textblah = new String(buffer, 0, numbytes);

                StringTokenizer token = new StringTokenizer(textblah);
                int numspaces = Integer.parseInt(token.nextToken(" "));
                for (int i = 0; i < numspaces; i++) {
                    String tok = token.nextToken(" ");

                    if (tok.contains("0")) {
                        spacelist.add(i, "#" + i + " is vacant");

                    } else {
                        spacelist.add(i, "#" + i + " is occupied");
                    }
                    adapter.notifyDataSetChanged();
                }

            }
        } catch (IOException e) {
            msg(e.toString());
        } catch (Exception e) {
            msg(e.toString());
        }
    }


    public void home(View v) {
        finish();
    }


    private class retrieveSpacesInfo extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            pd = ProgressDialog.show(startReservation.this, "Retrieving info...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... urls) {
            requestSpaces();


            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {

                    receiveSpaces();

                    pd.dismiss();


                }
            }, 3000); // 3000 milliseconds delay


        }

    }

    private class registerInfo extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            pd = ProgressDialog.show(startReservation.this, "Registering user...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... urls) {
            requestRegisterUser();


            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {

                    receiveRegisteredUser();

                    pd.dismiss();


                }
            }, 3000); // 3000 milliseconds delay


        }

    }


    public void requestRegisterUser() {

        if (btSocket != null) {
            Calendar c = Calendar.getInstance();
            SimpleDateFormat df = new SimpleDateFormat("HH:mm:ss");
            String startDate = df.format(c.getTime());
            try {
                out = btSocket.getOutputStream();
                out.flush();
                String output = plateNumber + " registeruser " + selectedposition + " " + startDate;
                out.write(output.getBytes());
            } catch (Exception e) {
                msg(e.toString());
            }
        }
    }

    public void receiveRegisteredUser() {
        byte[] buffer = new byte[256];  // buffer store for the stream
        if (btSocket != null) {
            try {
                in = btSocket.getInputStream();
                DataInputStream mmInStream = new DataInputStream(in);

                if (mmInStream.available() > 0) {
                    int numbytes = mmInStream.read(buffer);
                    String input = new String(buffer, 0, numbytes);
                    if (input.contains("success")) {
                        msg("Success. Door to "+ selectedposition + " has been opened");
                        msg("Door will close in 15s automatically");
                        finish();
                    } else if (input.contains("space")) {
                        msg("Space has become occupied. sorry");
                    }else{
                        msg("Numberplate already exists");
                    }
                }

            } catch (IOException e) {
                msg(e.toString());
            } catch (Exception e) {
                msg(e.toString());
            }
        }
    }


    public void registeruser(View v) {

        plateNumber = receiveabc.getText().toString();
        if (plateNumber.length() == 0) {
            msg("Nothing entered");
        } else if (plateNumber.length() < 4 || plateNumber.length() > 8) {
            msg("Incorrect length.");
        } else if (!plateNumber.matches("[A-Z0-9]+")) {
            msg("Ensure Uppercase letters and numbers are used");
        } else {
            if (selectedposition == -1) {
                msg("Parking space has not been selected or the current selected is occupied.");
            } else {
                new registerInfo().execute();
            }

        }
    }


    private void msg(String s) {
        Toast.makeText(getApplicationContext(), s, Toast.LENGTH_LONG).show();
    }
}



