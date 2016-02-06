package christensenjohnsrud.funfit;

import android.app.Activity;
import android.os.Handler;
import android.os.SystemClock;

/**
 * Created by siljechristensen on 05/02/16.
 */
public class Timer{

    private long startTime = 0L;
    private Handler customHandler = new Handler();
    long timeInMilliseconds = 0L;
    long timeSwapBuff = 0L;
    long updatedTime = 0L;

    public Runnable updateTimerThread = new Runnable() {
        public void run() {

            //TODO: Do what you need to do.
            timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
            updatedTime = timeSwapBuff + timeInMilliseconds;
            int secs = (int) (updatedTime / 1000);
            int mins = secs / 60;
            secs = secs % 60;
            int milliseconds = (int) (updatedTime % 1000);

            //The "trick"
            customHandler.postDelayed(this, 10); //This way the runnable is started every 10ms
        }
    };

    public long getUpdatedTime(){
        timeInMilliseconds = SystemClock.uptimeMillis() - startTime;
        updatedTime = timeSwapBuff + timeInMilliseconds;
        return updatedTime;
    }

    public int getSecs(){
        updatedTime = getUpdatedTime();
        int secs = (int) (updatedTime / 1000);
        secs = secs % 60;
        return secs;
    }

    public long getMins(){
        updatedTime = getUpdatedTime();
        int secs = (int) (updatedTime / 1000);
        int mins = secs / 60;
        return mins;
    }

    public int getMillis(){
        updatedTime = getUpdatedTime();
        int milliseconds = (int) (updatedTime % 1000);
        return  milliseconds;
    }





}
