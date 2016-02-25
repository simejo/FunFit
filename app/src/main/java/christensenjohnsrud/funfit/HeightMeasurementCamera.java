package christensenjohnsrud.funfit;

import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.media.FaceDetector;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;

/**
 * Created by siljechristensen on 23/02/16.
 */


public class HeightMeasurementCamera extends Activity{
    private Camera mCamera;
    private CameraPreview mPreview;

    private CameraPreview mCameraSurface;
    private FrameLayout mCameraFrame;
    private Button mCameraButton;
    private Bitmap mImage;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_height_measurement_camera);
        setupCamera();

    }
    @Override
    protected void onStop() {
        super.onStop();
        mCameraSurface.stopCamera();
    }

    private void setupCamera() {
        /* your code here:
            - create the new masked camera surface view with application context
            - create the new masked image view with application context
            - set masked image view scale type to FIT_XY
            - get the camera frame from the resource R.id.cameraDisplay
            - get the camera button from R.id.cameraButton
            - set the button on click method to be our cameraButtonOnClick() method
            - add the new masked camera surface and masked image view to camera frame
            - bring the camera surface view to front
        */
        mCameraSurface = new CameraPreview(this);
        mCameraFrame = (FrameLayout)findViewById(R.id.cameraDisplay);
        mCameraButton = (Button) findViewById(R.id.cameraButton);
        //mCameraButton.setOnClickListener(this);
        mCameraFrame.addView(mCameraSurface);
        mCameraSurface.bringToFront();
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

    }


}
