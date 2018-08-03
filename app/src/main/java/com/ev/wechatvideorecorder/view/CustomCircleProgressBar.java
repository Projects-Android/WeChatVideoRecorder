package com.ev.wechatvideorecorder.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.ColorInt;
import android.support.annotation.ColorRes;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.view.View;

/**
 * Created by EV on 2018/8/3.
 */

public class CustomCircleProgressBar extends View {

    private float mProgressAngle;
    private int mMax;

    private int mProgressWith;

    private Paint mPaintProgress;

    private @ColorInt
    int mProgressColor, mBgColor;

    private RectF mRectF;
    private int mCenter, mRadius;

    public CustomCircleProgressBar(Context context) {
        super(context);
    }

    public CustomCircleProgressBar(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomCircleProgressBar(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    public void init(int max, @ColorRes int progressColor, @ColorRes int bgColor, int progressWidth) {
        mMax = max;
        mProgressWith = progressWidth;
        mProgressColor = ContextCompat.getColor(getContext(), progressColor);
        mBgColor = ContextCompat.getColor(getContext(), bgColor);

        mPaintProgress = new Paint();
        mPaintProgress.setAntiAlias(true);
        mPaintProgress.setDither(true);
        mPaintProgress.setStrokeWidth(progressWidth);
    }

    public void setProgress(int progress) {
        mProgressAngle = (float) progress * 360 / mMax;

        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int measureWidth = MeasureSpec.getSize(widthMeasureSpec);
        int measureHeight = MeasureSpec.getSize(heightMeasureSpec);
        setMeasuredDimension(Math.min(measureWidth, measureHeight), Math.min(measureWidth, measureHeight));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (null == mRectF) {
            mCenter = this.getWidth() / 2;
            mRadius = mCenter - mProgressWith / 2;
            mRectF = new RectF(mProgressWith / 2, mProgressWith / 2, this.getWidth() - mProgressWith / 2, this.getWidth() - mProgressWith / 2);
        }

        mPaintProgress.setColor(mBgColor);
        mPaintProgress.setStyle(Paint.Style.STROKE);
        canvas.drawCircle(mCenter, mCenter, mRadius, mPaintProgress);

        mPaintProgress.setColor(mProgressColor);
        canvas.drawArc(mRectF, -90, mProgressAngle, false, mPaintProgress);
    }
}
