/*
* Copyright 2013 The Android Open Source Project
*
* Licensed under the Apache License, Version 2.0 (the "License");
* you may not use this file except in compliance with the License.
* You may obtain a copy of the License at
*
*     http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing, software
* distributed under the License is distributed on an "AS IS" BASIS,
* WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
* See the License for the specific language governing permissions and
* limitations under the License.
*/


package com.abs.mdu.bluetoothchat;

import android.os.Bundle;
import android.support.v4.app.FragmentTransaction;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.ViewAnimator;

import com.abs.mdu.common.activities.SampleActivityBase;
import com.abs.mdu.common.logger.Log;
import com.abs.mdu.common.logger.LogFragment;
import com.abs.mdu.common.logger.LogWrapper;
import com.abs.mdu.common.logger.MessageOnlyLogFilter;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;

import static java.nio.charset.Charset.defaultCharset;

/**
 * A simple launcher activity containing a summary sample description, sample log and a custom
 * {@link android.support.v4.app.Fragment} which can display a view.
 * <p>
 * For devices with displays with a width of 720dp or greater, the sample log is always visible,
 * on other devices it's visibility is controlled by an item on the Action Bar.
 */
public class MainActivity extends SampleActivityBase {

    public static final String TAG = "MainActivity";
    private SeekBar seekBar;
    private SeekBar seekBar2;
    private TextView textView;
    private TextView textView2;
    private EditText mOutEditText;
    private Button mSendButton;
    private int id;
    byte  start_byte;
    byte  setCurrent;
    byte  getCurrent;
    byte  setTemperature;
    byte  getTemperature;
    byte  opc;
    byte  status;

    // Whether the Log Fragment is currently shown
    private boolean mLogShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (savedInstanceState == null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            BluetoothChatFragment fragment = new BluetoothChatFragment();
            transaction.replace(R.id.sample_content_fragment, fragment);
            transaction.commit();
        }



        initializeVariables();

        // Initialize the textview with '0'.
        textView.setText(getString(R.string.current_string) + seekBar.getProgress());
        textView2.setText(getString(R.string.temperature_string) + seekBar.getProgress());

        seekBar2.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            float progress = 0.0f;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                progress = progresValue;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                textView2.setText(getString(R.string.temperature_string) + progress );

                try {
                    sendMsg(progress, setTemperature);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                progress = progresValue;
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                        textView.setText(getString(R.string.current_string) + progress );
                        mOutEditText = (EditText) findViewById(R.id.edit_text_out);
                try {
                    sendMsg(progress, setCurrent);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        });

    }       //end of onCreate

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem logToggle = menu.findItem(R.id.menu_toggle_log);
        logToggle.setVisible(findViewById(R.id.sample_output) instanceof ViewAnimator);
        logToggle.setTitle(mLogShown ? R.string.sample_hide_log : R.string.sample_show_log);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_toggle_log:
                mLogShown = !mLogShown;
                ViewAnimator output = (ViewAnimator) findViewById(R.id.sample_output);
                if (mLogShown) {
                    output.setDisplayedChild(1);
                } else {
                    output.setDisplayedChild(0);
                }
                supportInvalidateOptionsMenu();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Create a chain of targets that will receive log data */
    @Override
    public void initializeLogging() {
        // Wraps Android's native log framework.
        LogWrapper logWrapper = new LogWrapper();
        // Using Log, front-end to the logging chain, emulates android.util.log method signatures.
        Log.setLogNode(logWrapper);

        // Filter strips out everything except the message text.
        MessageOnlyLogFilter msgFilter = new MessageOnlyLogFilter();
        logWrapper.setNext(msgFilter);

        // On screen logging via a fragment with a TextView.
        LogFragment logFragment = (LogFragment) getSupportFragmentManager()
                .findFragmentById(R.id.log_fragment);
        msgFilter.setNext(logFragment.getLogView());

        Log.i(TAG, "Ready");
    }

    // A private method to help us initialize our variables.
    private void initializeVariables() {
        seekBar = (SeekBar) findViewById(R.id.seekBar1);
        seekBar2 = (SeekBar) findViewById(R.id.seekBar2);
        textView = (TextView) findViewById(R.id.textView1);
        textView2 = (TextView) findViewById(R.id.textView2);
        seekBar.setMax(600);
        seekBar2.setMax(70);
        id = 0;
        start_byte =    0x7E;
        setCurrent =    0x0A;
        getCurrent =    0x0B;
        setTemperature = 0x1A;
        getTemperature = 0x1B;
        opc =           0x70;
        status =        0x00;
    }

    private void sendMsg(float progress, byte cmd) throws IOException {

        mOutEditText = (EditText) findViewById(R.id.edit_text_out);



        byte[] temperature_msg = new byte[11];
        Arrays.fill( temperature_msg, (byte) 0 );

        temperature_msg[0] = start_byte;
        //length in the end
        temperature_msg[2] = opc;
        temperature_msg[3] = (byte) id;
        temperature_msg[4] = cmd;
        temperature_msg[5] = status;

        ByteBuffer buffer = ByteBuffer.allocate(4);
//b.order(ByteOrder.BIG_ENDIAN); // optional, the initial order of a byte buffer is always BIG_ENDIAN.

        buffer.putFloat(progress);
        byte[] byte_progress  = new byte[4];;
        Arrays.fill( byte_progress, (byte) 0 );
        byte_progress = buffer.array();
        temperature_msg[6] = (byte) byte_progress [3];
        temperature_msg[7] = (byte) byte_progress [2];
        temperature_msg[8] = (byte) byte_progress [1];
        temperature_msg[9] = (byte) byte_progress [0];
        temperature_msg[1] = (byte) (temperature_msg.length -2 ) ;

        byte crc, i;
        crc=0;
        for (i=0; i< temperature_msg.length; i++) {
            crc ^= temperature_msg[i];
        }

        temperature_msg[10] = ((byte) crc );
        Charset mycharset = defaultCharset();
        //length must be in the end, when msg is completely build;




        //String temperature_msg_string = Arrays.toString(temperature_msg);
        String temperature_msg_string = new String(temperature_msg, mycharset); //must put the correct encoding for the negatives
            if(temperature_msg_string.length() != 11 ) {
                temperature_msg_string = "Error size = " + temperature_msg_string.length();
            }

        id++;
        mOutEditText.setText(temperature_msg_string);
        mSendButton = (Button) findViewById(R.id.button_send);
        mSendButton.callOnClick();

    }

}
