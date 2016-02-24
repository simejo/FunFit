package christensenjohnsrud.funfit;

import android.app.Activity;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

public class HighMeasurementTesting extends Activity {

    TextView disp;
    private static int[] sampleRate = new int[] { 44100, 22050, 11025, 8000 };
    short audioData[];
    double finalData[];
    int bufferSize,srate;
    String TAG;
    public boolean recording;
    AudioRecord recorder;
    Complex[] fftArray;
    float freq;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_height_measurement);
        disp = (TextView) findViewById(R.id.textView_height_measurement);

        Thread t1 = new Thread(new Runnable(){

            public void run() {

                Log.i(TAG, "Setting up recording");
                for (int rate : sampleRate) {
                    try{

                        Log.d(TAG, "Attempting rate " + rate);

                        bufferSize=AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_CONFIGURATION_MONO,
                                AudioFormat.ENCODING_PCM_16BIT)*3; //get the buffer size to use with this audio record

                        if (bufferSize != AudioRecord.ERROR_BAD_VALUE) {

                            recorder = new AudioRecord (MediaRecorder.AudioSource.MIC,rate,AudioFormat.CHANNEL_CONFIGURATION_MONO,
                                    AudioFormat.ENCODING_PCM_16BIT,2048); //instantiate the AudioRecorder
                            Log.d(TAG, "BufferSize " +bufferSize);
                            srate = rate;

                        }

                    } catch (Exception e) {
                        Log.e(TAG, rate + "Exception, keep trying.",e);
                    }
                }
                bufferSize=2048;
                recording=true; //variable to use start or stop recording
                audioData = new short [bufferSize]; //short array that pcm data is put into.
                Log.i(TAG,"Got buffer size =" + bufferSize);
                while (recording) {  //loop while recording is needed
                    Log.i(TAG,"in while 1");
                    if (recorder.getState()==android.media.AudioRecord.STATE_INITIALIZED) // check to see if the recorder has initialized yet.
                        if (recorder.getRecordingState()==android.media.AudioRecord.RECORDSTATE_STOPPED)
                            recorder.startRecording();  //check to see if the Recorder has stopped or is not recording, and make it record.

                        else {
                            Log.i(TAG,"in else");
                            // audiorecord();
                            finalData=convert_to_double(audioData);
                            Findfft();
                            for(int k=0;k<fftArray.length;k++)
                            {
                                freq = ((float)srate/(float) fftArray.length) *(float)k;
                                runOnUiThread(new Runnable(){
                                    public void run()
                                    {
                                        disp.setText("The frequency is " + freq);
                                        if(freq>=15000)
                                            recording = false;
                                    }
                                });


                            }


                        }//else recorder started

                } //while recording

                if (recorder.getState()==android.media.AudioRecord.RECORDSTATE_RECORDING)
                    recorder.stop(); //stop the recorder before ending the thread
                recorder.release(); //release the recorders resources
                recorder=null; //set the recorder to be garbage collected.

            }//run

        });
        t1.start();
    }





    private void Findfft() {
        // TODO Auto-generated method stub
        Complex[] fftTempArray = new Complex[bufferSize];
        for (int i=0; i<bufferSize; i++)
        {
            fftTempArray[i] = new Complex(finalData[i], 0);
        }
        fftArray = FFT.fft(fftTempArray);
    }


    private double[] convert_to_double(short data[]) {
        // TODO Auto-generated method stub
        double[] transformed = new double[data.length];

        for (int j=0;j<data.length;j++) {
            transformed[j] = (double)data[j];
        }

        return transformed;

    }
}