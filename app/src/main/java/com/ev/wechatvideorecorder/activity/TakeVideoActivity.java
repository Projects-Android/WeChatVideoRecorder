package com.ev.wechatvideorecorder.activity;

import android.app.Activity;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.ev.wechatvideorecorder.R;
import com.ev.wechatvideorecorder.utils.CameraHelper;
import com.ev.wechatvideorecorder.view.VideoRecordButton;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Created by EV on 2018/7/19.
 */

public class TakeVideoActivity extends Activity implements VideoRecordButton.OnCountDownListener {
    private static final String TAG = TakeVideoActivity.class.getSimpleName();

    public static final int MAX_VIDEO_DURATION = 30 * 1000; // 30s
    public static final int MIN_VIDEO_DURATION = 3 * 1000; // 3s

    private SurfaceView mPreview;
    private VideoRecordButton mVideoRecordButton;
    private File mOutputFile;

    private MediaRecorder mMediaRecorder;
    private Camera mCamera;
    private CamcorderProfile mCamcorderProfile;

    private boolean mIsRecording;

    private int mCurRotation = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_take_video);

        initViews();
    }

    private void initViews() {
        mPreview = findViewById(R.id.tv_take_video);
        mPreview.getHolder().setKeepScreenOn(true);
        mPreview.getHolder().setFormat(PixelFormat.TRANSPARENT);
        mPreview.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                prepareCamera();
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                releaseCamera();
            }
        });

        mVideoRecordButton = findViewById(R.id.vrb_take_video);
        mVideoRecordButton.setOnCountDownOverListener(this);
    }

    private void setCameraDisplayOrientation(int cameraId, android.hardware.Camera camera) {
        if (null == camera) {
            return;
        }

        Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
        int rotation = getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else {
            // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        camera.setDisplayOrientation(result);
        mCurRotation = result;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        setCameraDisplayOrientation(0, mCamera);
    }

    private void start() {
        new MediaPrepareTask().execute(null, null, null);
    }

    private void stop() {
        if (mIsRecording) {
            try {
                mMediaRecorder.stop();
            } catch (RuntimeException e) {
                // RuntimeException is thrown when stop() is called immediately after start().
                // In this case the output file is not properly constructed ans should be deleted.
                mOutputFile.delete();
            }

            releaseMediaRecorder();
            mIsRecording = false;
        }
    }

    private void prepareCamera() {
        // BEGIN_INCLUDE (configure_preview)
        mCamera = CameraHelper.getDefaultCameraInstance();
        setCameraDisplayOrientation(0, mCamera);

        // We need to make sure that our preview and recording video size are supported by the
        // camera. Query camera to find all the sizes and choose the optimal size given the
        // dimensions of our preview surface.
        Camera.Parameters parameters = mCamera.getParameters();
        List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
        List<Camera.Size> mSupportedVideoSizes = parameters.getSupportedVideoSizes();
        Camera.Size optimalSize = CameraHelper.getOptimalVideoSize(mSupportedVideoSizes,
                mSupportedPreviewSizes, mPreview.getWidth(), mPreview.getHeight());

        // Use the same size for recording profile.
        CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        profile.videoFrameWidth = optimalSize.width;
        profile.videoFrameHeight = optimalSize.height;
        mCamcorderProfile = profile;

        // likewise for the camera object itself.
        parameters.setPreviewSize(profile.videoFrameWidth, profile.videoFrameHeight);
        List<String> focusMode = parameters.getSupportedFocusModes();
        if (focusMode.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            parameters.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
            mCamera.cancelAutoFocus();
        }
        mCamera.setParameters(parameters);
        try {
            // Requires API level 11+, For backward compatibility use {@link setPreviewDisplay}
            // with {@link SurfaceView}
            mCamera.setPreviewDisplay(mPreview.getHolder());
            mCamera.startPreview();
        } catch (Exception e) {
            Log.e(TAG, "Surface texture is unavailable or unsuitable" + e.getMessage());
        }
        // END_INCLUDE (configure_preview)
    }

    private boolean prepareVideoRecorder() {
        if (null == mCamera) {
            prepareCamera();
        }

        // BEGIN_INCLUDE (configure_media_recorder)
        mMediaRecorder = new MediaRecorder();
        mMediaRecorder.reset();
        try {
            mMediaRecorder.setOrientationHint(mCurRotation);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Step 1: Unlock and set camera to MediaRecorder
        mCamera.unlock();
        mMediaRecorder.setCamera(mCamera);

        // Step 2: Set sources
        mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.DEFAULT);
        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);

        // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
        mMediaRecorder.setProfile(mCamcorderProfile);

        // Step 4: Set output file
        mOutputFile = CameraHelper.getOutputMediaFile(CameraHelper.MEDIA_TYPE_VIDEO);
        if (mOutputFile == null) {
            return false;
        }
        mMediaRecorder.setOutputFile(mOutputFile.getPath());
        // END_INCLUDE (configure_media_recorder)

        // Step 5: Prepare configured MediaRecorder
        try {
            mMediaRecorder.prepare();
        } catch (IllegalStateException e) {
            Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (IOException e) {
            Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        } catch (Exception e) {
            Log.d(TAG, "Exception preparing MediaRecorder: " + e.getMessage());
            releaseMediaRecorder();
            return false;
        }
        return true;
    }

    private void releaseMediaRecorder() {
        if (mMediaRecorder != null) {
            // clear recorder configuration
            mMediaRecorder.reset();
            // release the recorder object
            mMediaRecorder.release();
            mMediaRecorder = null;
            // Lock camera for later use i.e taking it back from MediaRecorder.
            // MediaRecorder doesn't need it anymore and we will release it if the activity pauses.
            mCamera.lock();
        }
    }

    private void releaseCamera() {
        if (mCamera != null) {
            // release the camera for other applications
            mCamera.release();
            mCamera = null;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // if we are using MediaRecorder, release it first
        releaseMediaRecorder();
        // release the camera immediately on pause event
        releaseCamera();
    }

    /**
     * Asynchronous task for preparing the {@link android.media.MediaRecorder} since it's a long blocking
     * operation.
     */
    class MediaPrepareTask extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            // initialize video camera
            if (prepareVideoRecorder()) {
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                mMediaRecorder.start();

                mIsRecording = true;
            } else {
                // prepare didn't work, release the camera
                releaseMediaRecorder();
                releaseCamera();
                return false;
            }
            return true;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            if (!result) {
                TakeVideoActivity.this.finish();
            }
        }
    }

    @Override
    public void onCountDownStart() {
        start();
    }

    @Override
    public void onCountDownCancel(int reason) {
        switch (reason) {
            case VideoRecordButton.COUNTING_DOWN_CANCEL_REASON_CLOSE:
                finish();
                break;
            case VideoRecordButton.COUNTING_DOWN_CANCEL_REASON_ROLLBACK:
                break;
            case VideoRecordButton.COUNTING_DOWN_CANCEL_REASON_SIZE:
                Toast.makeText(this, "Video duration is too short!", Toast.LENGTH_SHORT).show();
                break;
            default:
                break;
        }
    }

    @Override
    public void onCountDownOver() {
        stop();
    }

    @Override
    public void onVideoConfirm() {
        // TODO: 2018/8/3 Send your video to other page or save it.
        finish();
    }
}
