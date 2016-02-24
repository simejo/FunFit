package christensenjohnsrud.funfit;

import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.media.ToneGenerator;
import android.os.Environment;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class HeightMeasurement extends AppCompatActivity implements View.OnClickListener{

    private Button btn_send_ping, play, record;
    private TextView tv_height_measurement;
    private AudioManager audioManager;
    private MediaRecorder mediaRecorder;
    private static String mFileName = null;

    private MediaRecorder myAudioRecorder;
    private String outputFile = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_height_measurement);

        play=(Button)findViewById(R.id.button3);
        record=(Button)findViewById(R.id.button_send_ping);

        play.setEnabled(false);
        outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.3gp";

        Log.i("External Storage", "Permission: " + isExternalStorageWritable() + "--" + Environment.getExternalStorageDirectory().getAbsolutePath().toString());


        myAudioRecorder=new MediaRecorder();
        myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
        myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
        myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);

        myAudioRecorder.setOutputFile(outputFile);

        record.setOnClickListener(this);

        play.setOnClickListener(this);
    }

    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        if (Environment.MEDIA_MOUNTED.equals(state)) {
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_send_ping){
            startRecord();
            int recordTime = 2000;
            Handler recordHandler = new Handler();

            recordHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopRecord();
                }
            }, recordTime);
        }

        else if(v.getId() == R.id.button3){
            MediaPlayer m = new MediaPlayer();

            try {
                m.setDataSource(outputFile);
            } catch (IOException e) {
                e.printStackTrace();
            }

            try {
                m.prepare();
            } catch (IOException e) {
                e.printStackTrace();
            }

            m.start();
            File file = new File(outputFile);
            Log.i("SOUND TO STRING", file.toString());
            Toast.makeText(getApplicationContext(), "Playing audio", Toast.LENGTH_LONG).show();
        }
    }

    public void startRecord(){
        try {
            myAudioRecorder.prepare();
            myAudioRecorder.start();
        }

        catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        record.setEnabled(false);

        Toast.makeText(getApplicationContext(), "Recording started", Toast.LENGTH_SHORT).show();
        ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME);
        toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000);
    }

    public void stopRecord(){
        myAudioRecorder.stop();
        myAudioRecorder.release();
        myAudioRecorder  = null;

        play.setEnabled(true);

        Toast.makeText(getApplicationContext(), "Audio recorded successfully",Toast.LENGTH_SHORT).show();
    }
}
