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
    private String pauseTime;
    private Integer[] totalTime = new Integer[]{0,0,0,0};


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
        handler.postDelayed(updateTimeTask, 0);
    }

    private Runnable updateTimeTask = new Runnable() {
        public void run() {
            final long start = startTime;

            convertTimeToArray(SystemClock.uptimeMillis() - start);
            tv.setText(getCurrentTime());

            handler.postAtTime(this, start + (((totalTime[1] * 60) + totalTime[2]+ 1) * 1000));

        }
    };

    public void setCurrentTime(String currentTime){
        this.currentTime = currentTime;
        tv.setText(currentTime);
    }

    public String getCurrentTime() {
        String holder = totalTime[0] + "";
        if (totalTime[1] < 10) {
            holder += (":0" + totalTime[1]);

        } else {
            holder += (":" + totalTime[1]);
        }
        if (totalTime[2] < 10) {
            holder += (":0" + totalTime[2]);

        } else {
            holder += (":" + totalTime[2]);
        }
        return holder;

    }

    public void setStartTime(long startTime){
        this.startTime = startTime;
    }

    public void resetTimer(){
        setStartTime(SystemClock.uptimeMillis());
        removeHandlerCallback();
        postDelayed();
    }

    public void pause(){
        this.removeHandlerCallback();
    }

    public void start(){
        if(totalTime[0] == totalTime[1] && totalTime[2] == totalTime[3] && totalTime[0] == totalTime[3] && totalTime[3] == 0){
            setStartTime(SystemClock.uptimeMillis());
        }
        else{
            setStartTime(SystemClock.uptimeMillis() - convertArrayToLong());
        }
        postDelayed();
    }

    public void finish() {
        removeHandlerCallback();
        totalTime[0] = 0;
        totalTime[1] = 0;
        totalTime[2] = 0;
        totalTime[3] = 0;
        setCurrentTime("00:00:00");


    }

    public void convertTimeToArray(long totalMillis) {
        int seconds = (int) (totalMillis / 1000);
        int minutes = seconds / 60;
        seconds = seconds % 60;
        int hours = (int) minutes / 60;
        minutes = minutes % 60;
        totalTime[0] = hours;
        totalTime[1] = minutes;
        totalTime[2] = seconds;
        totalTime[3] = (int) (totalMillis - seconds * 1000 - minutes * 1000 * 60 - hours * 1000 * 60 * 60);
    }

    public long convertArrayToLong(){
        return totalTime[0]*1000*60*60 + totalTime[1]*1000*60 + totalTime[2]*1000 + totalTime[3];
    }


}
