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
import android.widget.TextView;

public class measure extends Activity implements OnClickListener {
	public final static String SHARED_PREFS_NAME="sonarsettings";
    sonicPing sp;
    CheckBox cb;
    AudioManager am;
    String unit = "m";
    float SoS = 340.f;
    boolean pinging = false;
    Button button;
	String TAG = "measure.java";
	TextView tv_height;

    @Override
    public void onConfigurationChanged(Configuration config) {
    	super.onConfigurationChanged(config);
    }
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
    	Log.d(TAG, "onCreate()");
        super.onCreate(savedInstanceState);
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
        
		//Create sp from settings
		sp = new sonicPing();
        am = ((AudioManager)this.getSystemService(AUDIO_SERVICE));

        setContentView(R.layout.activity_height_measurement);
        button = (Button)findViewById(R.id.button_send_ping);
        button.setOnClickListener(this);
        cb = (CheckBox)findViewById(R.id.CheckMaxVol);
		tv_height = (TextView) findViewById(R.id.textView_height_measurement);
    }

    

    public void onClick(View v) {
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

		int counter = 0;
		for (float[] x : sp.getDistanceList()) {
			Log.d(TAG + " peaks", counter + ": " + x + "    " + ((Float) (Math.round(x[0] * 100) / 100.f)).toString());
		}
		float[][] results = sp.getDistanceList();
		float final_height = ((Float) (Math.round(results[0][0] * 100) / 100.f));
		tv_height.setText(final_height + "");


		if (pinging && oldVolume > -100)
			am.setStreamVolume(AudioManager.STREAM_MUSIC, oldVolume, 0);
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