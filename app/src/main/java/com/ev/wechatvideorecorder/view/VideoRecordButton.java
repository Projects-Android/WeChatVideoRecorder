package com.ev.wechatvideorecorder.view;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;

import com.ev.wechatvideorecorder.R;
import com.ev.wechatvideorecorder.activity.TakeVideoActivity;

/**
 * Created by EV on 2018/8/3.
 */

public class VideoRecordButton extends FrameLayout implements Animator.AnimatorListener, View.OnTouchListener, View.OnLongClickListener, View.OnClickListener {

    private static final int MSG_UPDATE_PROGRESS = 0x01;
    private static final int MSG_PROGRESS_DONE = 0x02;

    private static final int DURATION_ANIMATION = 200;

    public static final int COUNTING_DOWN_CANCEL_REASON_SIZE = 0x01;
    public static final int COUNTING_DOWN_CANCEL_REASON_ROLLBACK = 0x02;
    public static final int COUNTING_DOWN_CANCEL_REASON_CLOSE = 0x03;

    private View mOutSideCircle, mInsideCircle, mBtnClose, mBtnRollBack, mBtnConfirm;
    private CustomCircleProgressBar mProgressBar;
    private int mCurrentProgress;

    private boolean mStop;
    private OnCountDownListener mOnCountDownListener;

    private AnimatorSet mOutsideAnim, mInsideAnim;
    private ObjectAnimator mOutsideAnimX, mOutsideAnimY, mInsideAnimX, mInsideAnimY;

    private boolean mIsRecording;
    private long mRecordStartTime;

    public VideoRecordButton(Context context) {
        super(context);

        initialize(context);
    }

    public VideoRecordButton(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);

        initialize(context);
    }

    public VideoRecordButton(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);

        initialize(context);
    }

    private void initialize(Context context) {
        removeAllViews();
        LayoutInflater.from(context).inflate(R.layout.layout_view_video_record_button, this);

        mBtnClose = findViewById(R.id.iv_video_record_close);
        mBtnConfirm = findViewById(R.id.iv_video_record_confirm);
        mBtnRollBack = findViewById(R.id.iv_video_record_rollback);
        mOutSideCircle = findViewById(R.id.civ_video_record_outside);
        mInsideCircle = findViewById(R.id.civ_video_record_inside);
        mProgressBar = findViewById(R.id.ccpb_video_record_progress);
        mProgressBar.init(TakeVideoActivity.MAX_VIDEO_DURATION - DURATION_ANIMATION, R.color.pb_color, R.color.transparent, 14);

        mOutsideAnim = new AnimatorSet();
        mInsideAnim = new AnimatorSet();

        mOutsideAnimX = ObjectAnimator.ofFloat(mOutSideCircle, "scaleX", 1f, 1.5f);
        mOutsideAnimY = ObjectAnimator.ofFloat(mOutSideCircle, "scaleY", 1f, 1.5f);
        mInsideAnimX = ObjectAnimator.ofFloat(mInsideCircle, "scaleX", 1f, 0.8f);
        mInsideAnimY = ObjectAnimator.ofFloat(mInsideCircle, "scaleY", 1f, 0.8f);

        mOutsideAnim.play(mOutsideAnimX).with(mOutsideAnimY);
        mInsideAnim.play(mInsideAnimX).with(mInsideAnimY);
        mOutsideAnim.setDuration(DURATION_ANIMATION);
        mInsideAnim.setDuration(DURATION_ANIMATION);
        mInsideAnim.addListener(this);

        mBtnClose.setOnClickListener(this);
        mBtnConfirm.setOnClickListener(this);
        mBtnRollBack.setOnClickListener(this);
        mOutSideCircle.setOnTouchListener(this);
        mOutSideCircle.setOnLongClickListener(this);
        mIsRecording = false;
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        mStop = false;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mStop = true;
    }

    public void setOnCountDownOverListener(OnCountDownListener onCountDownListener) {
        mOnCountDownListener = onCountDownListener;
    }

    private void start() {
        mIsRecording = true;
        mStop = false;
        mOutsideAnim.start();
        mInsideAnim.start();
    }

    private void recover() {
        mIsRecording = false;
        mStop = true;
        mCurrentProgress = 0;
        mProgressBar.setProgress(0);

        mOutSideCircle.setVisibility(VISIBLE);
        mInsideCircle.setVisibility(VISIBLE);
        mBtnRollBack.setVisibility(GONE);
        mBtnConfirm.setVisibility(GONE);

        mOutSideCircle.setScaleX(1);
        mOutSideCircle.setScaleY(1);
        mInsideCircle.setScaleX(1);
        mInsideCircle.setScaleY(1);
    }

    private void showOperation() {
        recover();

        mOutSideCircle.setVisibility(GONE);
        mInsideCircle.setVisibility(GONE);
        mBtnRollBack.setVisibility(VISIBLE);
        mBtnConfirm.setVisibility(VISIBLE);
    }

    private void updateProgressPerSecond() {
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (!mStop) {
                    mCurrentProgress += 50;
                    if (mCurrentProgress < TakeVideoActivity.MAX_VIDEO_DURATION) {
                        mHandler.sendEmptyMessage(MSG_UPDATE_PROGRESS);
                    } else {
                        mHandler.sendEmptyMessage(MSG_PROGRESS_DONE);
                    }
                }
            }
        }, 50);
    }

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (!mStop) {
                if (msg.what == MSG_UPDATE_PROGRESS) {
                    mProgressBar.setProgress(mCurrentProgress);
                    updateProgressPerSecond();
                } else if (msg.what == MSG_PROGRESS_DONE) {
                    mProgressBar.setProgress(TakeVideoActivity.MAX_VIDEO_DURATION);
                    if (null != mOnCountDownListener) {
                        mOnCountDownListener.onCountDownOver();
                        showOperation();
                    }
                }
            }
        }
    };

    @Override
    public void onAnimationStart(Animator animation) {
        mRecordStartTime = System.currentTimeMillis();

        if (null != mOnCountDownListener) {
            mOnCountDownListener.onCountDownStart();
        }
    }

    @Override
    public void onAnimationEnd(Animator animation) {
        mProgressBar.setProgress(0);
        updateProgressPerSecond();
    }

    @Override
    public void onAnimationCancel(Animator animation) {

    }

    @Override
    public void onAnimationRepeat(Animator animation) {

    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_UP:
                if (mIsRecording) {
                    if (null != mOnCountDownListener) {
                        mOnCountDownListener.onCountDownOver();
                    }

                    long duration = System.currentTimeMillis() - mRecordStartTime;
                    if (duration < TakeVideoActivity.MIN_VIDEO_DURATION) {
                        recover();
                        if (null != mOnCountDownListener) {
                            mOnCountDownListener.onCountDownCancel(COUNTING_DOWN_CANCEL_REASON_SIZE);
                        }
                    } else {
                        showOperation();
                    }
                }
                break;
        }
        return false;
    }

    @Override
    public boolean onLongClick(View v) {
        start();
        return false;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_video_record_close:
                if (null != mOnCountDownListener) {
                    mOnCountDownListener.onCountDownCancel(COUNTING_DOWN_CANCEL_REASON_CLOSE);
                }
                break;
            case R.id.iv_video_record_confirm:
                if (null != mOnCountDownListener) {
                    mOnCountDownListener.onVideoConfirm();
                }
                break;
            case R.id.iv_video_record_rollback:
                recover();
                if (null != mOnCountDownListener) {
                    mOnCountDownListener.onCountDownCancel(COUNTING_DOWN_CANCEL_REASON_ROLLBACK);
                }
                break;
            default:
                break;
        }
    }

    public interface OnCountDownListener {
        void onCountDownStart();
        void onCountDownCancel(int reason);
        void onCountDownOver();
        void onVideoConfirm();
    }
}
