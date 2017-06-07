package com.ezpark;

import android.app.Activity;
import android.os.Bundle;


/**
 * Created by David on 25/05/2017.
 */

/*
This is run when the user's android device does not support bluetooth
 */

public class nobluetooth extends Activity {

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.nobluetooth);

    }


}
