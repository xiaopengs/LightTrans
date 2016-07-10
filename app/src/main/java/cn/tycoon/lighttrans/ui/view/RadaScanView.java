package cn.tycoon.lighttrans.ui.view;

import java.util.HashSet;
import java.util.Map;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff.Mode;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Scroller;

import cn.tycoon.lighttrans.R;
import cn.tycoon.lighttrans.utils.DisplayUtil;
import cn.tycoon.lighttrans.utils.ImageUtils;

/**
 * 雷达扫描动画
 * 
 * @author zijianlu
 */
public class RadaScanView extends SurfaceView implements SurfaceHolder.Callback {
    protected static final String TAG = "RadaScanView";

    protected Bitmap mScanBmp;
    protected Rect mScanDstRect;
    protected float mScanDegree;
    protected Paint mScanPaint;

    protected int mCenterX;
    protected int mCenterY;

    protected Rect mCenterFaceDstRect;
    protected Bitmap mCenterFaceBitmap;
    protected int mCenterFaceRadius;
    protected Paint mCenterFaceBgPaint;

    protected String mUin;
    protected Context mContext;
    protected SurfaceHolder mHolder;

    protected HandlerThread mDrawThread;
    protected WorkHandler mDrawHandler;
    protected static final int MSG_REFRESH = 1;
    protected volatile boolean isStoped = false;

    public RadaScanView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;

        mHolder = this.getHolder();// 获取holder
        mHolder.addCallback(this);
        setZOrderOnTop(true);
        mHolder.setFormat(PixelFormat.TRANSPARENT);
        mDrawThread = new HandlerThread("RadaScanView");
        mDrawThread.start();
        mDrawHandler = new WorkHandler(mDrawThread.getLooper());

        initScan();

        initSelection(context);
    }

    protected class WorkHandler extends Handler {
        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            doRefreshDraw();
        }
    };

    public void setApp() {
        if (mCenterFaceBitmap == null) {
            mCenterFaceBitmap = ImageUtils.drawableToBitmap(getResources().getDrawable(R.drawable.device_icon));
        }
    }

    protected long mDrawCount = 0;
    protected static final long MIN_DELAY = 15;

    /** 画图 */
    protected void doRefreshDraw() {

        long t1 = System.currentTimeMillis();

        Surface surface = mHolder.getSurface();
        if (!isStoped && surface != null && surface.isValid()) {
            Canvas canvas = null;
            try {
                canvas = mHolder.lockCanvas(null);// 获取画布
                if (canvas == null) {
                    return;
                }
                canvas.drawColor(Color.TRANSPARENT, Mode.CLEAR);

                drawRadaScan(canvas);

                drawCenterFace(canvas);

                drawSelectionFace(canvas);

                //drawArrow(canvas);

            } catch (Exception e) {
                e.printStackTrace();

                reportException("scanDrawdExp", e.toString(), isStoped + "_" + mDrawCount);

            } finally {
                try {
                    if (canvas == null){
                        return;
                    }
                    boolean isValid = true;
                    // bug-51017793，在4.2.2的机器上，调用这个方法有用户上报crash
                    if (Build.VERSION.SDK_INT != 17) {
                        isValid = surface.isValid();
                    }

                    if (isValid) {
                        mHolder.unlockCanvasAndPost(canvas);

                        long t2 = System.currentTimeMillis();
                        long cost = t2 - t1;
                        if (cost < MIN_DELAY) {
                            try {
                                Thread.sleep(MIN_DELAY - cost);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }

                        mDrawCount++;

                        if (!isStoped) {
                            mDrawHandler.removeMessages(MSG_REFRESH);
                            mDrawHandler.sendEmptyMessage(MSG_REFRESH);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();

                    boolean isValid = (canvas != null);
                    if (isValid && Build.VERSION.SDK_INT != 17) {
                        isValid = surface.isValid();
                    }

                    reportException("scanUnLockExp", e.toString(), isStoped + "_" + mDrawCount + "_" + isValid);
                }
            }
        }
    }

    /** 上报异常 */
    protected void reportException(String expType, String expInfo, String extra) {
        try {
            StringBuilder sbd = new StringBuilder(extra);
            sbd.append("_");
            sbd.append(mSurfaceCreatedCount);
            sbd.append("_");
            sbd.append(mSurfaceChangedCount);
            sbd.append("_");
            sbd.append(mSurfaceDestroyCount);
        } catch (Exception e) {
        }
    }

    protected int mCicleSpace;
    protected int mEdgePading;
    protected int mScanRadius;

    protected int mSurfaceChangedCount = 0;
    protected int mSurfaceCreatedCount = 0;
    protected int mSurfaceDestroyCount = 0;

    public void setSpace(int pading, int space, int scanRadius) {
        mCicleSpace = space;
        mEdgePading = pading;
        mScanRadius = scanRadius;
        mScanDstRect.set(mCenterX, mEdgePading + mCicleSpace, getWidth() - mEdgePading - mCicleSpace, mCenterY);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        mCenterX = width / 2;
        mCenterY = height / 2;
        mScanDstRect.set(mCenterX, mEdgePading + mCicleSpace, width - mEdgePading - mCicleSpace, mCenterY);

        mCenterFaceDstRect.set(mCenterX - mCenterFaceRadius, mCenterY - mCenterFaceRadius,
                mCenterX + mCenterFaceRadius, mCenterY + mCenterFaceRadius);

        mSurfaceChangedCount++;
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {
        startDraw();
        mSurfaceCreatedCount++;
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopDraw();
        mSurfaceDestroyCount++;
    }

    public void startDraw() {
        isStoped = false;
        mDrawHandler.removeMessages(MSG_REFRESH);

        // 4.3绘制延迟一段时间
        if (Build.VERSION.SDK_INT == 18) {
            mDrawHandler.sendEmptyMessageDelayed(MSG_REFRESH, 450);
        } else {
            mDrawHandler.sendEmptyMessageDelayed(MSG_REFRESH, 150);
        }
    }

    public void stopDraw() {
        isStoped = true;
        mDrawHandler.removeMessages(MSG_REFRESH);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }

    protected void destroy() {
        isStoped = true;

        mDrawHandler.removeMessages(MSG_REFRESH);
        mDrawThread.quit();
    }

    protected void initScan() {
        mScanDstRect = new Rect();

        mScanPaint = new Paint();
        mScanPaint.setAntiAlias(true);
        mScanPaint.setFilterBitmap(true);

        mCenterFaceBgPaint = new Paint();
        mCenterFaceBgPaint.setColor(Color.WHITE);
        mCenterFaceBgPaint.setAntiAlias(true);
        mCenterFaceBgPaint.setStyle(Paint.Style.STROKE);
        mCenterFaceBgPaint.setStrokeWidth(DisplayUtil.dip2px(mContext, 1));

        mCenterFaceDstRect = new Rect();
        mCenterFaceRadius = DisplayUtil.dip2px(mContext, 20); // 头像大小40dp
    }

    protected void drawRadaScan(Canvas canvas) {
        if (mScanBmp == null) {
            long t1 = System.currentTimeMillis();
            try {
                mScanBmp = BitmapFactory.decodeResource(getResources(), R.drawable.search_device_radar_bg);

            } catch (Throwable e) {
                mScanBmp = null;
            }
        }

        if (mScanBmp != null) {
            canvas.save();
            canvas.rotate(mScanDegree, mCenterX, mCenterY);
            mScanDegree += 1.8;
            canvas.drawBitmap(mScanBmp, null, mScanDstRect, mScanPaint);
            canvas.restore();

            if (mDegreeChangeListener != null) {
                mDegreeChangeListener.onDegreeChage(mScanDegree);
            }
        }
    }

    protected void drawCenterFace(Canvas canvas) {
        if (mCenterFaceBitmap != null) {
            canvas.drawBitmap(mCenterFaceBitmap, null, mCenterFaceDstRect, mScanPaint);
            mScanPaint.setColor(Color.BLACK);
            canvas.drawCircle(mCenterX, mCenterY, DisplayUtil.dip2px(mContext, 21), mCenterFaceBgPaint);
        }
    }

    protected OnScanDegreeChangeListener mDegreeChangeListener;

    public void setOnScanDegreeChangeListener(OnScanDegreeChangeListener listener) {
        mDegreeChangeListener = listener;
    }

    public static interface OnScanDegreeChangeListener {
        public void onDegreeChage(float degree);
    }

    protected Paint mSelectFaceBgPaint;
    protected Paint mSelectFacePaint;

    protected Scroller mScroller;
    protected Point mSelectPoint;
    protected Point mLastSelectPoint;

    protected long mSelectUin = 0;
    protected long mLastSelectUin = 0;

    protected int mSelectFaceSize = 0;
    protected int mLastFaceSize = 0;
    protected int mMaxFaceSize = 0;
    protected Rect mSelectFaceDesRect;

    protected Bitmap mSelectBmp;
    protected Bitmap mLastSelectBmp;
    protected Bitmap mDefaultFaceBmp;

    protected volatile boolean isSelectInScanArea = false;

    protected Object mSelectionLock = new Object();

    private void initSelection(Context context) {
        mScroller = new Scroller(context);
        mMaxFaceSize = DisplayUtil.dip2px(context, 15);

        mSelectPoint = new Point();
        mLastSelectPoint = new Point();
        mSelectFaceDesRect = new Rect();

        mSelectFacePaint = new Paint();
        mSelectFacePaint.setAntiAlias(true);
        mSelectFacePaint.setFilterBitmap(true);

        mSelectFaceBgPaint = new Paint();
        mSelectFaceBgPaint.setAntiAlias(true);
        mSelectFaceBgPaint.setColor(Color.parseColor("#5affffff"));
        mSelectFaceBgPaint.setStyle(Style.STROKE);
        mSelectFaceBgPaint.setStrokeWidth(DisplayUtil.dip2px(context, 1));

        mDefaultFaceBmp = ImageUtils.drawableToBitmap(getResources().getDrawable(R.drawable.device_icon));
    }

    public void setSelection(long uin, HashSet<Long> inScanAreaMemberSet, Map<Long, Point> positions) {
        synchronized (mSelectionLock) {
            if (uin == mSelectUin) {
                return;
            }

            mLastSelectUin = mSelectUin;
            mSelectUin = uin;

            isSelectInScanArea = inScanAreaMemberSet.contains(mSelectUin);
            mSelectPoint = positions.get(mSelectUin);

            if (inScanAreaMemberSet.contains(mLastSelectUin)) {
                mLastSelectPoint = positions.get(mLastSelectUin);
            } else {
                mLastSelectPoint = null;
            }

            mLastSelectBmp = mSelectBmp;

            if (mSelectBmp == null) {
                mSelectBmp = mDefaultFaceBmp;
            }

            mScroller.abortAnimation();
            mScroller.startScroll(0, 0, mMaxFaceSize, 0, 400);
        }
    }

    public void reset() {
        synchronized (mSelectionLock) {
            mSelectBmp = null;
            mLastSelectBmp = null;
            mSelectUin = 0;
            isSelectInScanArea = false;
        }
    }

    protected boolean isScaling = false;

    public void onScaleChange(boolean isScale, HashSet<Long> inScanAreaMemberSet, Map<Long, Point> positions) {
        synchronized (mSelectionLock) {
            isScaling = isScale;

            if (isScaling) {
                mScroller.abortAnimation();
            }

            isSelectInScanArea = inScanAreaMemberSet.contains(mSelectUin);
            mSelectPoint = positions.get(mSelectUin);

            if (inScanAreaMemberSet.contains(mLastSelectUin)) {
                mLastSelectPoint = positions.get(mLastSelectUin);
            } else {
                mLastSelectPoint = null;
            }
        }
    }

    public void drawSelectionFace(Canvas canvas) {
        synchronized (mSelectionLock) {
            if (mScroller.computeScrollOffset()) {
                mSelectFaceSize = mScroller.getCurrX();
                mLastFaceSize = mMaxFaceSize - mScroller.getCurrX();
            }
            float minFactor = 0.2f;

            if (mSelectBmp != null && isSelectInScanArea && mSelectPoint != null) {
                mSelectFaceDesRect.set(mSelectPoint.x - mSelectFaceSize, mSelectPoint.y - mSelectFaceSize,
                        mSelectPoint.x + mSelectFaceSize, mSelectPoint.y + mSelectFaceSize);

                float factor = mSelectFaceSize * 1f / mMaxFaceSize;
                if (factor < minFactor) {
                    factor = 0;
                }
                mSelectFacePaint.setAlpha((int) (factor * 0xff));
                mSelectFaceBgPaint.setAlpha((int) (factor * 0x5a));

                canvas.drawBitmap(mSelectBmp, null, mSelectFaceDesRect, mSelectFacePaint);
                canvas.drawCircle(mSelectPoint.x, mSelectPoint.y, mSelectFaceSize, mSelectFaceBgPaint);

            } else {
            }

            if (mLastFaceSize > 0 && mLastSelectPoint != null && mLastSelectBmp != null && isSelectInScanArea) {
                mSelectFaceDesRect.set(mLastSelectPoint.x - mLastFaceSize, mLastSelectPoint.y - mLastFaceSize,
                        mLastSelectPoint.x + mLastFaceSize, mLastSelectPoint.y + mLastFaceSize);

                float factor = mLastFaceSize * 1f / mMaxFaceSize;
                if (factor < minFactor) {
                    factor = 0;
                }
                mSelectFacePaint.setAlpha((int) (factor * 0xff));
                mSelectFaceBgPaint.setAlpha((int) (factor * 0x5a));

                canvas.drawBitmap(mLastSelectBmp, null, mSelectFaceDesRect, mSelectFacePaint);
                canvas.drawCircle(mLastSelectPoint.x, mLastSelectPoint.y, mLastFaceSize, mSelectFaceBgPaint);
            }
        }
    }

}
