package christensenjohnsrud.funfit;

import android.app.Activity;
import android.content.Context;
import android.hardware.Camera;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

/**
 * Created by siljechristensen on 23/02/16.
 */
public class CameraPreview extends SurfaceView implements SurfaceHolder.Callback{
    private static final int LANDSCAPE_ROTATE = 0;
    private static final int PORTRAIT_ROTATE = 90;

    private final SurfaceHolder mHolder;
    private String className = "MaskCameraSufraceView.java";


    /* Use android.hardware.Camera2 if you are going for a higher version number! */
    private Camera mCamera;

    private int mRotation = 0;

    public CameraPreview(final Context context) {
        super(context);
        mHolder = getHolder();
        mHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        mCamera = Camera.open();

        try {
            mCamera.setPreviewDisplay(holder);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mCamera.stopPreview();

        final Camera.Parameters parameters = mCamera.getParameters();
        final Camera.Size previewSize = parameters.getSupportedPreviewSizes().get(0);
        parameters.setPreviewSize(previewSize.width, previewSize.height);

        if (width > height) {
            mCamera.setDisplayOrientation(LANDSCAPE_ROTATE);
            mRotation = LANDSCAPE_ROTATE;
        }
        else {
            mCamera.setDisplayOrientation(PORTRAIT_ROTATE);
            mRotation = PORTRAIT_ROTATE;
        }

        mCamera.setParameters(parameters);
        mCamera.startPreview();
    }

    @Override
    public void surfaceDestroyed(final SurfaceHolder holder) {
        if (mCamera != null){
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void capture(final Camera.PictureCallback imageHandler) {
        /* your code here: take picture with camera and call the passed imageHandler */
        mCamera.takePicture(null, null, imageHandler);

    }


    public void startPreview() {
        /* your code here: start preview of camera */
        if (mCamera != null){
            mCamera.startPreview();
        }
    }

    public int getCurrentRotation() {
        /* your code here: get the current rotation */
        //NOT REALLY USING
        Camera.CameraInfo info = new Camera.CameraInfo();
        Activity activity = (Activity)getContext();
        int rotation =  activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0: degrees = PORTRAIT_ROTATE; break; //Natural orientation
            case Surface.ROTATION_90: degrees = LANDSCAPE_ROTATE; break; //Landscape left
            case Surface.ROTATION_180: degrees = PORTRAIT_ROTATE; break;//Upside down
            case Surface.ROTATION_270: degrees = -LANDSCAPE_ROTATE; break;//Landscape right
        }
        return degrees;
    }

    public void stopCamera() {
        if (mCamera != null) {
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }
}
