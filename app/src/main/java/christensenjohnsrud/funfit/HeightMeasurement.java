package christensenjohnsrud.funfit;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.AudioTrack;
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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class HeightMeasurement extends AppCompatActivity implements View.OnClickListener{

    private Button btn_send_ping, play, record;
    private TextView tv_height_measurement;
    private AudioManager audioManager;
    private MediaRecorder mediaRecorder;
    private static String mFileName = null;

    private AudioRecord audioInput;
    private String outputFile = null;

    int channel_config = AudioFormat.CHANNEL_CONFIGURATION_MONO;
    int format = AudioFormat.ENCODING_PCM_16BIT;
    int sampleSize = 8000;
    int bufferSize = 1024 * 2;
    short[] audioBuffer;
    private boolean isRecording;
    private Thread recordingThread;
    byte bData[];
    double dData[];


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_height_measurement);

        play=(Button)findViewById(R.id.button3);
        record=(Button)findViewById(R.id.button_send_ping);

        play.setEnabled(false);
        //outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/recording.3gp";
        outputFile = Environment.getExternalStorageDirectory().getAbsolutePath() + "/voice8K16bitmono.pcm";

        Log.i("External Storage", "Permission: " + isExternalStorageWritable() + "--" + Environment.getExternalStorageDirectory().getAbsolutePath().toString());

        audioInput = new AudioRecord(MediaRecorder.AudioSource.MIC, sampleSize, channel_config, format, bufferSize);
        isRecording = false;

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

            try {
                PlayShortAudioFileViaAudioTrack(outputFile);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Toast.makeText(getApplicationContext(), "Playing audio", Toast.LENGTH_LONG).show();
        }
    }

    public void startRecord(){
        try {
            audioBuffer = new short[bufferSize];
            audioInput.startRecording();
            audioInput.read(audioBuffer, 0, bufferSize);
        }

        catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        record.setEnabled(false);

        Toast.makeText(getApplicationContext(), "Recording started", Toast.LENGTH_SHORT).show();
        //ToneGenerator toneG = new ToneGenerator(AudioManager.STREAM_ALARM, ToneGenerator.MAX_VOLUME);
        //toneG.startTone(ToneGenerator.TONE_CDMA_ALERT_CALL_GUARD, 1000);

        /*audioInput.startRecording();
        isRecording = true;
        recordingThread = new Thread(new Runnable() {
            public void run() {
                writeAudioDataToFile();
            }
        }, "AudioRecorder Thread");
        recordingThread.start();*/
    }

    public void stopRecord(){
        if (null != audioInput) {
            isRecording = false;
            audioInput.stop();
            audioInput.release();
            audioInput = null;
            recordingThread = null;
        }
        play.setEnabled(true);

        Toast.makeText(getApplicationContext(), "Audio recorded successfully",Toast.LENGTH_SHORT).show();
    }

    private double[] convert_short_to_double(short data[]) {

        // TODO Auto-generated method stub
        double[] transformed = new double[data.length];

        for (int j=0;j<data.length;j++) {
            transformed[j] = (double)data[j];
        }
        return transformed;
    }

    private byte[] convert_short_to_byte(short[] sData) {
        int shortArrsize = sData.length;
        byte[] bytes = new byte[shortArrsize * 2];
        for (int i = 0; i < shortArrsize; i++) {
            bytes[i * 2] = (byte) (sData[i] & 0x00FF);
            bytes[(i * 2) + 1] = (byte) (sData[i] >> 8);
            sData[i] = 0;
        }
        return bytes;
    }

    private void writeAudioDataToFile() {
        // Write the output audio in byte


        short sData[] = new short[1024];

        FileOutputStream os = null;
        try {
            os = new FileOutputStream(outputFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        while (isRecording) {
            // gets the voice output from microphone to byte format

            audioInput.read(sData, 0, 1024);
            try {
                // // writes the data to file from buffer
                // // stores the voice buffer
                bData = convert_short_to_byte(sData);
                dData = convert_short_to_double(sData);
                Log.i("WRITE TO FILE converted","converted to byte" + bData.toString());

                os.write(bData, 0, 1024 * 2);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        try {
            os.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        findFFT();
    }

    private void PlayShortAudioFileViaAudioTrack(String filePath) throws IOException
    {
        // We keep temporarily filePath globally as we have only two sample sounds now..
        if (filePath==null)
            return;

        //Reading the file..
        byte[] byteData = null;
        File file = null;
        file = new File(filePath); // for ex. path= "/sdcard/samplesound.pcm" or "/sdcard/samplesound.wav"
        byteData = new byte[(int) file.length()];
        FileInputStream in = null;
        try {
            in = new FileInputStream( file );
            in.read( byteData );
            in.close();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        // Set and push to audio track..
        int intSize = android.media.AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_8BIT);
        AudioTrack at = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                AudioFormat.ENCODING_PCM_8BIT, intSize, AudioTrack.MODE_STREAM);
        if (at!=null) {
            at.play();
            // Write the byte array to the track
            at.write(byteData, 0, byteData.length);
            at.stop();
            at.release();
        }
        else
            Log.d("TCAudio", "audio track is not initialised ");
    }

    private void findFFT(){
        Complex[] fftTempArray = new Complex[bufferSize];
        for (int i=0; i<bufferSize; i++)
        {
            fftTempArray[i] = new Complex(dData[i], 0);
        }
        Complex[] fftArray = FFT.fft(fftTempArray);
        int counter = 1;

        for(Complex x : fftArray){
            //Log.i("" + counter, x.toString());
            Complex newX = x.times(x);
            double magnitude = Math.sqrt(newX.re() + newX.im());
            if(!Double.isNaN(magnitude)){
                Log.i("magnitude" + counter, (int)magnitude + ", from: " + x.toString());
                counter++;
            }
        }
    }
}
