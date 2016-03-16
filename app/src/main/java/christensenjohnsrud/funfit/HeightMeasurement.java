package christensenjohnsrud.funfit;

import android.app.Activity;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.TextView;

import java.text.DecimalFormat;

public class HeightMeasurement extends Activity implements OnClickListener {
    private Ping sp;
	private CheckBox cb;
	private AudioManager am;
	private String unit = "centimeter";
	private boolean pinging = false;
	private Button button;
	String TAG = "HeightMeasurement.java";
	TextView tv_height, tv_height1, tv_height2, tv_height3, tv_height4, tv_height5;
	RadioButton radio_centimeter, radio_inch;
	private float unitFactor = 100;
	DecimalFormat format = new DecimalFormat("0.00");

	@Override
    public void onConfigurationChanged(Configuration config) {
    	super.onConfigurationChanged(config);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
		sp = new Ping();
        am = ((AudioManager)this.getSystemService(AUDIO_SERVICE));

        setContentView(R.layout.activity_height_measurement);
        button = (Button)findViewById(R.id.button_send_ping);
        button.setOnClickListener(this);
        cb = (CheckBox)findViewById(R.id.CheckMaxVol);
		tv_height1 = (TextView) findViewById(R.id.textView_height_measurement1);
		tv_height2 = (TextView) findViewById(R.id.textView_height_measurement2);
		tv_height3 = (TextView) findViewById(R.id.textView_height_measurement3);
		tv_height4 = (TextView) findViewById(R.id.textView_height_measurement4);
		tv_height5 = (TextView) findViewById(R.id.textView_height_measurement5);

		radio_centimeter = (RadioButton) findViewById(R.id.radioCentimeter);
		radio_inch = (RadioButton) findViewById(R.id.radioInch);

		radio_centimeter.setOnClickListener(this);
		radio_inch.setOnClickListener(this);
		radio_centimeter.setChecked(true);
    }

    

    public void onClick(View v) {
		if(v.getId() == R.id.button_send_ping){
			Log.d(TAG, "onClick)");
			int oldVolume = -100;
			if (!pinging && cb.isChecked()) {
				oldVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
				am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
			}

			float[] res = sp.ping();
			if (res == null) {
				tv_height.setText("Please ping again");
			}

			float[][] results = sp.getDistanceList();

			tv_height1.setText("1. " + format.format(results[0][0]*unitFactor) + " " + unit);
			tv_height2.setText("2. " + format.format(results[1][0]*unitFactor) + " " + unit);
			tv_height3.setText("3. " + format.format(results[2][0]*unitFactor) + " " + unit);
			tv_height4.setText("4. " + format.format(results[3][0]*unitFactor) + " " + unit);
			tv_height5.setText("5. " + format.format(results[4][0]*unitFactor) + " " + unit);

			if (pinging && oldVolume > -100){
				am.setStreamVolume(AudioManager.STREAM_MUSIC, oldVolume, 0);
			}
		} else if(v.getId() == R.id.radioCentimeter){
			unit = "meter";
			unitFactor = 100;
		} else if (v.getId() == R.id.radioInch){
			unit = "inches";
			unitFactor = 39.3701f;
		}
    }
    
    public void onDestroy() {
    	Log.d(TAG, "onDestroy()");
    	super.onDestroy();
    	pinging = false;
    	button.setText("Ping");
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    	Log.d(TAG, "onWindowsFocusChanged()");
    	if (!hasFocus) {
        	pinging = false;
        	button.setText("Ping");
    	}
    }
}