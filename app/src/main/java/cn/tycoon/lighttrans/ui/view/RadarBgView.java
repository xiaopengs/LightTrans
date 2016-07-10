package cn.tycoon.lighttrans.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Scroller;

import cn.tycoon.lighttrans.utils.DisplayUtil;

/**
 * 雷达扫描的背景视图，主要用于扫描动画
 * 
 * @author sidwang
 */
public class RadarBgView extends View {
    public static final int MARGIN = 15;

    private int mMargin;
    private Paint mPaint = new Paint();
    private int mWidth;

    private int mSpace;

    private int mInnerRadius;
    private int mOuterRadius;
    private OnBackgroundReadyListener mOnReadyListener;

    protected int mCenterFaceRadiusPx;

    protected Scroller mScroller;

    public RadarBgView(Context context) {
        super(context);
        init(context);
    }

    public RadarBgView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RadarBgView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context ctx) {
        mMargin = DisplayUtil.dip2px(getContext(), MARGIN);
        mCenterFaceRadiusPx = DisplayUtil.dip2px(getContext(), 20);
        // 画5个圆
        mPaint.setColor(0xEC7070);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeWidth(2);
        mPaint.setAntiAlias(true);
        mPaint.setShader(null);

        mScroller = new Scroller(ctx);
    }

    public void setOnBackgroundReadyListenerr(OnBackgroundReadyListener listener) {
        this.mOnReadyListener = listener;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int thisWidth = getWidth() - 2 * mMargin;
        int centerX = thisWidth / 2 + mMargin;
        int centerY = thisWidth / 2 + mMargin;

        if (mWidth != thisWidth) {
            mWidth = thisWidth;
            mOuterRadius = mWidth / 2;
            mSpace = (mOuterRadius - mCenterFaceRadiusPx) / 5;
            // space = 2 * outerRadius / 11;
            mInnerRadius = mOuterRadius - 4 * mSpace;

            if (mOnReadyListener != null) {
                mOnReadyListener.onReady(mMargin, mCenterFaceRadiusPx * 2, mInnerRadius, mOuterRadius, mOuterRadius
                        - mSpace, mSpace);
            }

            mCurOuterRadius = mOuterRadius;
        }

        mPaint.setAlpha((int) (0xff * 0.15));
        if (mCurOuterRadius <= mOuterRadius) {
            canvas.drawCircle(centerX, centerY, mCurOuterRadius, mPaint);
        }

        mPaint.setAlpha((int) (0xff * 0.25));
        canvas.drawCircle(centerX, centerY, mCurOuterRadius - mSpace, mPaint);

        mPaint.setAlpha((int) (0xff * 0.5));
        canvas.drawCircle(centerX, centerY, mCurOuterRadius - 2 * mSpace, mPaint);

        mPaint.setAlpha((int) (0xff * 0.75));
        canvas.drawCircle(centerX, centerY, mCurOuterRadius - 3 * mSpace, mPaint);

        canvas.drawCircle(centerX, centerY, mCurOuterRadius - 4 * mSpace, mPaint);

        canvas.drawCircle(centerX, centerY, mCurOuterRadius - 5 * mSpace, mPaint);
        // canvas.drawCircle(centerX, centerY, outerRadius - 5 * space, paint);
    }

    int mCurOuterRadius;
    int mLastDistance = 0;

    public void onScaleChange(int action, int actionDistance) {
        switch (action) {
        case MotionEvent.ACTION_POINTER_DOWN:
            mLastDistance = actionDistance;

            break;
        case MotionEvent.ACTION_MOVE:
            int delta = ((actionDistance - mLastDistance) / 5) % (mSpace);
            mCurOuterRadius += delta;

            if (delta > 0) {
                // 放大
                if (mCurOuterRadius >= mOuterRadius + mSpace) {
                    mCurOuterRadius = mOuterRadius;
                }
            } else {
                // 缩小
                if (mCurOuterRadius <= mOuterRadius - mSpace) {
                    mCurOuterRadius = mOuterRadius;
                }

            }
            mLastDistance = actionDistance;
            invalidate();

            break;

        case MotionEvent.ACTION_CANCEL:
        case MotionEvent.ACTION_POINTER_UP:
            if (mCurOuterRadius != mOuterRadius) {
                mScroller.abortAnimation();
                if (mCurOuterRadius > mOuterRadius) {
                    mScroller.startScroll(mCurOuterRadius, 0, mOuterRadius + mSpace - mCurOuterRadius, 0, 500);
                } else {
                    mScroller.startScroll(mCurOuterRadius, 0, mOuterRadius - mSpace - mCurOuterRadius, 0, 500);
                }

                invalidate();
            }

            break;

        }
    }

    @Override
    public void computeScroll() {
        if (mScroller.computeScrollOffset()) {
            mCurOuterRadius = mScroller.getCurrX();
            if (mCurOuterRadius >= mOuterRadius + mSpace) {
                mCurOuterRadius = mOuterRadius;

            } else if (mCurOuterRadius <= mOuterRadius - mSpace) {
                mCurOuterRadius = mOuterRadius;
            }

            invalidate();
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 确保始终是方形的
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    public static interface OnBackgroundReadyListener {
        void onReady(int padding, int centerImgSize, int innnerRadius, int outerRadius, int scanRadius, int space);
    }

}
