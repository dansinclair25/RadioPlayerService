package co.mobiwise.myapplication;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import co.mobiwise.library.radio.RadioListener;
import co.mobiwise.library.radio.RadioManager;

/**
 * Created by mertsimsek on 04/11/15.
 */
public class RadioActivity extends Activity implements RadioListener{

    private final String[] RADIO_URL = {"http://hayatmix.net/;yayin.mp3.m3u"};

    Button mButtonControlStart;
    TextView mTextViewControl;
    RadioManager mRadioManager = RadioManager.with(this);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_radio);


        mRadioManager.registerListener(this);
        mRadioManager.setLogging(true);

        initializeUI();
    }

    public void initializeUI() {
        mButtonControlStart = (Button) findViewById(R.id.buttonControlStart);
        mTextViewControl = (TextView) findViewById(R.id.textviewControl);

        mButtonControlStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mRadioManager.isPlaying())
                    mRadioManager.startRadio(RADIO_URL[0]);
                else
                    RadioManager.getService().setCloseNotificationOnStop(true);
                    mRadioManager.stopRadio();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();
        mRadioManager.connect();
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mRadioManager.disconnect();
    }

    @Override
    public void onRadioLoading() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //TODO Do UI works here.
                mTextViewControl.setText("RADIO STATE : LOADING...");
            }
        });
    }

    @Override
    public void onRadioConnected() {

    }

    @Override
    public void onRadioStarted() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //TODO Do UI works here.
                mTextViewControl.setText("RADIO STATE : PLAYING...");
            }
        });
    }

    @Override
    public void onRadioStopped() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                //TODO Do UI works here
                mTextViewControl.setText("RADIO STATE : STOPPED.");
            }
        });
    }

    @Override
    public void onMetaDataReceived(String s, String s1) {
        //TODO Check metadata values. Singer name, song name or whatever you have.
    }

    @Override
    public void onError() {

    }
}
