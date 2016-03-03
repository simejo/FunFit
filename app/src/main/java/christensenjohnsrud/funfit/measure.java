package christensenjohnsrud.funfit;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.TextView;

public class measure extends Activity implements OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {
	public final static String SHARED_PREFS_NAME="sonarsettings";
    sonicPing sp;
    CheckBox cb;
    AudioManager am;
    String unit = "m";
    float SoS = 340.f;
    boolean contMode = false;
    boolean pinging = false;
    Button button;
	String TAG = "measure.java";
	TextView tv_height;

    private final Handler mHandler = new Handler();
    private final Runnable contPing = new Runnable() {
		public void run() {
			Log.d(TAG, "contPing.run()");
			float[] res = sp.ping();
			if (res == null) {
				if (sp.error == -5) {
					dieWithError("Recording failed. (contPing), recRes = "+sp.error_detail);
				} else if (sp.error == -6) {
					dieWithError("startRecording failed. (contPing). Maybe the mic is already in use? If not:");
				} else
					dieWithError("Unknown error in ping(). (contPing), error = " + sp.error_detail + ", detail = " + sp.error_detail);
				return;
			}
			mHandler.removeCallbacks(contPing);
			if (pinging) {
				mHandler.postDelayed(contPing, 100);
			}
		}
	};
/*	private final Runnable autoKill = new Runnable() {
		public void run() {
			Log.d(TAG, "autoKill.run()");
			dieWithError("Automatic self-termination. Hope this works :)");
		}
	};*/
    
    private SharedPreferences mPrefs;
	
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
//        sp = new sonicPing();
        am = ((AudioManager)this.getSystemService(AUDIO_SERVICE));
        
        
        setContentView(R.layout.activity_height_measurement);
        
        button = (Button)findViewById(R.id.button_send_ping);
        button.setOnClickListener(this);

        cb = (CheckBox)findViewById(R.id.CheckMaxVol);
        
        mPrefs = measure.this.getSharedPreferences(SHARED_PREFS_NAME, 0);
		mPrefs.registerOnSharedPreferenceChangeListener(this);
		tv_height = (TextView) findViewById(R.id.textView_height_measurement);
		onSharedPreferenceChanged(mPrefs, null);
    }

    

    public void onClick(View v) {
    	Log.d(TAG, "onClick)");
//    	mHandler.postDelayed(autoKill, 5000);
    	int oldVolume = -100;
    	if ((!contMode || !pinging) && cb.isChecked()) {
    		oldVolume = am.getStreamVolume(AudioManager.STREAM_MUSIC);
    		am.setStreamVolume(AudioManager.STREAM_MUSIC, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
    	}
    	if (!contMode) {
    		float[] res = sp.ping();
    		if (res == null) {
				if (sp.error == -5) {
					dieWithError("Recording failed. (singlePing), recRes = "+sp.error_detail);
				} else if (sp.error == -6) {
					dieWithError("startRecording failed. (singlePing). Maybe the mic is already in use? If not:");
				} else
					dieWithError("Unknown error in ping(). (singlePing), error = " + sp.error_detail + ", detail = " + sp.error_detail);
				return;
			}

			int counter = 0;
			for (float[] x : sp.getLastDistance()){
				Log.d(TAG + " peaks", counter + ": " + x + "    " + ((Float) (Math.round(x[0] * 100) / 100.f)).toString());
			}
			float[][] results = sp.getLastDistance();
			float final_height = ((Float) (Math.round(results[0][0] * 100) / 100.f));
			tv_height.setText(final_height + "");

    	}
    	if ((!contMode || pinging) && oldVolume > -100)
    		am.setStreamVolume(AudioManager.STREAM_MUSIC, oldVolume, 0);
    	if (contMode) {
    		if (pinging) { //Stop it
    			pinging = false;
    			mHandler.removeCallbacks(contPing);
    			button.setText("Ping");
    		} else { //Start i
    			pinging = true;
    			mHandler.postDelayed(contPing, 100);
    			button.setText("Stop");
    		}
    	}
    }
    
    private void dieWithError(String error) {
    	Log.d(TAG, "dieWithError()");
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setMessage(error + "\nPlease report this error and your device to DiConX@gmail.com");
		builder.setCancelable(false);
		builder.setNeutralButton("OK", new DialogInterface.OnClickListener() {
		           public void onClick(DialogInterface dialog, int id) {
		                measure.this.finish();
		           }
		       });
		AlertDialog x = builder.create();
		x.show();
    }
    
    public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
    	Log.d(TAG, "onSharedPreferenceChanged()");

    	if (prefs.getBoolean("continuous", false)) {
    		contMode = true;
    		sp = new sonicPing(1, 100, 3000, 2000, 250, 2);
    	} else {
    		contMode = false;
    		sp = new sonicPing();
    	}
    	
    	if (sp == null) {
    		dieWithError("The class sonicPing could not be created.");
    		return;
    	}
    	
    	if (sp.error < 0) {
    		switch (sp.error) {
    		case -1:	dieWithError("No suitable frequency found.");
    					return;
    		case -2:	dieWithError("Could not create Audio Track.");
						return;
    		case -3:	dieWithError("Could not fill Audio Track.");
						return;
    		case -4:	dieWithError("Could not create AudioRecord.");
						return;
    		}
    	}
    	
    	unit = prefs.getString("units", "m");
    	try {
    		SoS = Float.valueOf(prefs.getString("SoS", "340"));
    	} catch (Exception e) {
    		SoS = 340.f;
    	}
    	sp.setDistFactor(unit, SoS);
    	
    	pinging = false;
    	button.setText("Ping");
    	mHandler.removeCallbacks(contPing);
    	
    	sp.setCamMic(prefs.getBoolean("camMic", true));
	}
    
    public void onDestroy() {
    	Log.d(TAG, "onDestroy()");
    	super.onDestroy();
    	pinging = false;
    	button.setText("Ping");
    	mHandler.removeCallbacks(contPing);
    	mPrefs.unregisterOnSharedPreferenceChangeListener(this);
    }
    
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
    	Log.d(TAG, "onWindowsFocusChanged()");
    	if (!hasFocus) {
        	pinging = false;
        	button.setText("Ping");
        	mHandler.removeCallbacks(contPing);
    	}
    }
}