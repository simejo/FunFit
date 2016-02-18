package christensenjohnsrud.funfit;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.widget.TextView;

/**
 * Created by siljechristensen on 05/02/16.
 */
public class Timer extends Activity{

    private String className = "Timer.java";
    private long startTime = 0L;
    private String currentTime;
    private Handler handler = new Handler();


    private int textField;
    private TextView tv;
    private Context context;

    public Timer(Context context, int textField){
        if(handler == null){
            handler = new Handler();
        }
        this.currentTime = "";
        this.startTime = 0;
        this.textField = textField;
        this.context = context;
        this.tv = (TextView)((Activity)context).findViewById(textField);

    }

    public void removeHandlerCallback(){
        handler.removeCallbacks(updateTimeTask);
    }
    public void postDelayed(){
        handler.postDelayed(updateTimeTask, 10);
    }

    private Runnable updateTimeTask = new Runnable() {
        public void run() {
            final long start = startTime;
            long millis = SystemClock.uptimeMillis() - start;
            int seconds = (int) (millis / 1000);
            int minutes = seconds / 60;
            seconds     = seconds % 60;

            //TODO: ADD TIDELER
            if (seconds < 10) {
                setCurrentTime("" + minutes + ":0" + seconds);

            } else {
                setCurrentTime("" + minutes + ":" + seconds);
            }
            handler.postAtTime(this,
                    start + (((minutes * 60) + seconds + 1) * 1000));

        }
    };

    public void setCurrentTime(String currentTime){
        this.currentTime = currentTime;
        tv.setText(currentTime);    }

    public String getCurrentTime(){
        return currentTime;
    }

    public void setStartTime(long startTime){
        this.startTime = startTime;
    }


}
