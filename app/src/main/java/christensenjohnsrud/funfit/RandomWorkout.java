package christensenjohnsrud.funfit;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.NumberPicker;
import android.widget.TextView;

import java.util.Random;

/**
 * Created by siljechristensen on 06/03/16.
 */
public class RandomWorkout extends Activity implements SensorEventListener {

    private String className = "RandomWorkout";
    private static int SHAKE_THRESHOLD = 15;
    private SensorManager sensorManager;
    private Sensor accelerometerSensor;
    private Boolean blocker = false;
    private Handler handler;

    // NB! Remember to update all the lists
    private String[] core = {
            "Plank for 1 minunte",
            "Scissor for 1 min",
            "60 sit ups",
            "60 crunches",
            "20 vertical leg crunches",
            "12 V-ups",
            "20 raised knee-in",
            "12 Reverse crunches",
            "16 Flutter kicks",
            "Side plank 1 minute each side",
            "10 Lying side crunches each side",
            "20 Russian twists",
            "Plank 1 minute",
            "Spider plank 1 minute"};
    private String[] legs = {
            "20 Lunges",
            "20 Squats",
            "12 burpees",
            "5 pistol squats each leg",
            "10 box jumps",
            "10 lunges",
            "5 pistol squats",
            "15 sumo squats",
            "10 side lunges",
            "16 jump lunges",
            "12 squat jumps",
            "12 long jumps",
            "10 box jumps",
            "180* switch jumps",
            "12 glute bridges",
            "8 Bulgarian Lunges"};
    private String[] arms = {
            "Biceps curl",
            "Max pull-ups",
            "10 dips",
            "Max chin-ups",
            "Diamond pushups"};
    private String[] back = {
            "12 Back lifts",
            "6 dead lifts",
            "6 hang-ups",
            "Dying whale for 1 min",
            "Dying fish for 1 min",
            "Mountain climber for 1 min",
            "Superman plank for 1 min",
            "Plank knee tucks for 1 min",
            "Plank reaches for 1 min",
            "6 Pull ups",
            "Star plank for 1 min"};

    private String[][] types = {core, legs, arms, back};            // Needed for acceleration
    private String[] np_types = {"core","legs", "arms", "back"};   // Needed for numberPicker

    private int chosenType;

    private NumberPicker type_picker;
    private TextView exercise_tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_random_workout);

        // Setting up accelerometer sensor
        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometerSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorManager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL);

        // Help variable
        chosenType = 1;

        // NumberPicker
        type_picker = (NumberPicker) findViewById(R.id.type_picker);
        type_picker.setMinValue(0);                 // From array first value
        type_picker.setMaxValue(types.length - 1);  // To array last value
        type_picker.setWrapSelectorWheel(true);
        type_picker.setDisplayedValues(np_types);


        exercise_tv = (TextView) findViewById(R.id.text_view_exercise);

        type_picker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker picker, int oldVal, int newVal) {
                switch (newVal) {
                    case 1:
                        exercise_tv.setText(R.string.shake);
                        chosenType = 1;
                        break;
                    case 2:
                        exercise_tv.setText(R.string.shake);
                        chosenType = 2;
                        break;
                    case 3:
                        exercise_tv.setText(R.string.shake);
                        chosenType = 3;
                        break;
                    case 4:
                        exercise_tv.setText(R.string.shake);
                        chosenType = 4;
                        break;
                }
            }
        });

    }

    private String getRandomWorkout(String[] type){
        Random generator = new Random();
        int i = generator.nextInt(type.length);
        return type[i];
    }


    @Override
    public void onSensorChanged(SensorEvent event) {
        // Axis of the rotation sample, not normalized yet.
        if(!blocker){
            blocker = true;
            blockTimerCheck();
            float axisX = event.values[0];
            float axisY = event.values[1];
            float axisZ = event.values[2];
            if (axisX >= SHAKE_THRESHOLD || axisY >= SHAKE_THRESHOLD || axisZ >= SHAKE_THRESHOLD){
                String[] exerciseList = types[chosenType];
                exercise_tv.setText(getRandomWorkout(exerciseList));
            }
        }
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
    }

    private void blockTimerCheck(){
        handler = null;
        handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                blocker = false;
            }
        }, 750); // Possibility to change this. {"100","200","500", "1000", "1500", "2000"} ms

    }
}
