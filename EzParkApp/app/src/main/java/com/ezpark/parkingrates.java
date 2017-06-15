package com.ezpark;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import java.util.ArrayList;

/**
 * Created by David on 31/05/2017.
 */

/*Displays the set parking rates
* gets the rates from MyApp.java
* */

public class parkingrates extends Activity {

    private static  double[] rateList;
    private static String[] ratetiers;
    private static int numratestiers;

    ListView listRates;
    MyApp app;

    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.parkingrates);
        listRates = (ListView)findViewById(R.id.ratelist);

        app=(MyApp)getApplicationContext();
        ratetiers=app.getRatetiers();
        rateList=app.getRateList();
        numratestiers = rateList.length;
        listRates();

    }


    public void listRates() {
        ArrayList<String> list = new ArrayList<String>();
        for (int i = 0; i < numratestiers;i++){
            list.add(i, ratetiers[i] + " : $" + rateList[i]);
        }

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1,list);

        listRates.setAdapter(adapter);

    }

    public void home(View v){
        finish();
    }



}
