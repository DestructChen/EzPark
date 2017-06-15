package com.ezpark;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothSocket;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.StringTokenizer;
import java.util.concurrent.TimeUnit;


/**
 * Created by David on 1/06/2017.
 */

/*
User must enter their numberplate, click get payment info to retrieve data
payment info is displayed, user clicks pay
no payment verficiation has been implemented and it is automatically accepted when the user clicks on Pay now
after "paying", the button to open door is shown.
 */


public class endReservation extends Activity{
    ProgressDialog pd;
    private MyApp app;
    BluetoothSocket btSocket = null;
    InputStream in;
    OutputStream out;
    public String command = "";

    int tier = -1;
    double[] ratelist;
    String plateNumber = null;

    public ListView lv;
    EditText et;
    TextView payfinish;

    long hours;
    long minutes;

    String startTimeString = "";
    String endTimeString = "";
    String stringDouble = "";
    String stringDuration = "";

    public ArrayAdapter<String> adapter;
    public ArrayList<String> list;






    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.endreservation);


        lv = (ListView)findViewById(R.id.paymentlist);

        app =  (MyApp)getApplicationContext();
        ratelist = app.getRateList();

        try{
            btSocket=app.getBtSocket();
        }catch(NullPointerException e){
            msg("please connect to an arduino module");
            finish();
        }
        pd = new ProgressDialog(endReservation.this);
        list = new ArrayList<String>();

        et = (EditText)findViewById(R.id.textView2);
        list.add(0, "Start Time : "+  startTimeString); //default text
        list.add(1, "End Time : " +   endTimeString);
        list.add(2, "Duration : " +   stringDuration);
        list.add(3, "Payment Total : $" + stringDouble);
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,list);
        lv.setAdapter(adapter);
    }
    //when get payment info button is clicked, check for input
    public void requestInfo(View v){
        plateNumber = et.getText().toString();
        if(plateNumber.length()==0 ) {
            msg("Nothing entered");
        }else if(plateNumber.length()<4 || plateNumber.length()>8 ) {
            msg("Incorrect length.");
        }else if(!plateNumber.matches("[A-Z0-9]+")){
                msg("Ensure Uppercase letters and numbers are used");
        }else{

            new retrieveInfo().execute();


        }


    }
    //executes tasks in order
    private class retrieveInfo extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute()
        {
            pd = ProgressDialog.show(endReservation.this, "Retrieving info...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... urls) {
            requestDuration();


            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {

                   receiveDuration();

                    pd.dismiss();

                   listInfo();

                }
            }, 3000); // 3000 milliseconds delay


        }

    }
    //displays info, updates when called.
    public void listInfo() {

            list.set(0, "Start Time : " + startTimeString); //default text
            list.set(1, "End Time : " + endTimeString);
            list.set(2, "Duration : " + stringDuration);
            list.set(3, "Payment Total : $" + stringDouble);
        adapter.notifyDataSetChanged();

    }
    //request start time from arduino
    public void requestDuration(){


        if (btSocket!=null)
        {
            try {

                out = btSocket.getOutputStream();



                command = plateNumber + " getStart";

                out.write(command.getBytes());
                out.flush();




            } catch (IOException e) {
                msg(e.toString());
            }catch(Exception e){
                msg(e.toString());
            }
        }

    }
    //get data from arduino, calculate payment info, then display
    public void receiveDuration(){

        if (btSocket != null) {
            try{
                in = btSocket.getInputStream();
            DataInputStream mmInStream = new DataInputStream(in);
                 if(in.available()>0 || mmInStream.available() >0){
                        byte[] buffer = new byte[256];
                        Calendar c = Calendar.getInstance();
                        SimpleDateFormat format = new SimpleDateFormat("HH:mm:ss");
                        endTimeString = format.format(c.getTime());


                        int numbytes = mmInStream.read(buffer);

                        String textblah = new String(buffer, 0, numbytes);
                        StringTokenizer token = new StringTokenizer(textblah);
                        startTimeString = token.nextToken();
                        if(startTimeString.matches(".*\\d+.*")) {


                            Date start = format.parse(startTimeString);
                            Date end = format.parse(endTimeString);
                            long difference = end.getTime() - start.getTime();
                            long duration = TimeUnit.MILLISECONDS.toHours(difference);

                            minutes = (difference / (1000 * 60)) % 60;
                            hours = (difference / (1000 * 60 * 60)) % 24;
                            stringDuration = String.format("%02d hours %02d mins", hours, minutes);

                            if (duration <= 2.00) {
                                tier = 0;
                            } else if (duration > 2.00 && duration < 4.00) {
                                tier = 1;
                            } else if (duration > 4.00 && duration < 6.00) {
                                tier = 2;
                            } else if(duration>6.00) {
                                tier = 3;
                            }
                            if (tier != -1) {
                                stringDouble = Double.toString(ratelist[tier]);
                            }
                            listInfo();
                        }else{
                            String check = "numberplate";
                            if(startTimeString.equals(check)) {
                                msg("Numberplate not found");

                                list.set(0, "Start Time : " + " "); //default text
                                list.set(1, "End Time : " + " "); //default text
                            }
                        }}


            } catch (IOException e) {
                msg(e.toString());
            } catch (Exception e) {
                msg(e.toString());
            }

        }
    }

    //user ends reservation, checks if user has retrieved payment info yet
    //no real payment system, after 3s delay, goes to open door page
    public void pay(View v) {
        if(plateNumber==null){
            msg("please enter plate number");
        }else if(!lv.getItemAtPosition(3).toString().matches(".*\\d+.*")){
            msg("Invalid payment amount. please get payment info.");
        }else{
        try {
            pd = new ProgressDialog(endReservation.this);
            pd.setMessage("Processing payment");
            pd.show();

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    pd.dismiss();
                }
            }, 2000); // 3000 milliseconds delay
            setContentView(R.layout.paysuccess);
            payfinish = (TextView) findViewById(R.id.payfinish);
            payfinish.setText("Your payment of $" + stringDouble + " has been received.");

        }catch(Exception e){
        msg(e.toString());
    }}

    }



    public void openDoor(View v){
        new openDoorSequence().execute();
        msg("Door will close in 15s automatically");
    }
    //notify arduino, reservation has ended
    public void requestOpenDoor() {
        if (btSocket != null) {
            try {
                out = btSocket.getOutputStream();
                command = plateNumber + " paymentfinish";
                out.flush();
                out.write(command.getBytes());
            } catch (IOException e) {
                msg(e.toString());
            }
        }
    }

    //receive confirmation door has been opened
    public void receiveDoorStatus(){
        if(btSocket!=null){
            try {

                in = btSocket.getInputStream();
                byte[] buffer = new byte[256];

                DataInputStream mmInStream = new DataInputStream(in);

                if(in.available()>0) {

                    int numbytes = mmInStream.read(buffer);
                    String textblah = new String(buffer, 0, numbytes);
                    String check = "dooropened";

                    if(textblah.equals(check)){
                        msg("Door has been opened.");
                        finish();

                    }
                }

            }catch(IOException e){
                msg(e.toString());
            }
        }
    }

    private class openDoorSequence extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute()
        {
            pd = new ProgressDialog(endReservation.this);
            pd.setMessage("Opening Door");
            pd.show();
        }

        @Override
        protected Void doInBackground(Void... urls) {
            requestOpenDoor();


            return null;
        }

        @Override
        protected void onPostExecute(Void result) {

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {

                    receiveDoorStatus();

                    pd.dismiss();



                }
            }, 2000); // 3000 milliseconds delay

        }

    }


    //return to home
    public void Cancel(View v){
        try {
            finish();
        }catch(Exception e){
            msg(e.toString());
        }
    }


    //displays s as a message on screen
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }


}
