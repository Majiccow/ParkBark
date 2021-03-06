package com.team2.csc413.parkbark;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

public class SplashScreen extends Activity {


    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);

        final Context c = this;
        Thread time = new Thread() {
            public void run() {
                try {

                    sleep(4000);

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } finally {
                    Intent openStartingPoint = new Intent(c, MainActivity.class);
                    startActivity(openStartingPoint);
                }
            }
        };
        time.start();
    }

    protected void onPause() {
        super.onPause();
        finish();
    }

}