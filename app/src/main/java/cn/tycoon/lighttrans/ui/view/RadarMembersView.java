package cn.tycoon.lighttrans.ui.view;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.AttributeSet;
import android.view.GestureDetector;
import android.view.GestureDetector.OnGestureListener;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;

import cn.tycoon.lighttrans.data.NearbyMember;
import cn.tycoon.lighttrans.utils.DisplayUtil;

/**
 * 附近的群友的雷达上标记成员的View
 * 
 * @author zijianlu
 */
public class RadarMembersView extends View {
    public static final String TAG = "RadarMembersView";
    public static final int CIRCLE_RADIUS = 7;
    protected static final int EXTRA_CLICK_RADIUS = 3;

    public static final int ORIGIN_LIMIT_DISATNCE_VALUE = 10;// 单位km
    public static final double ORIGIN_SHOW_RANGE = ORIGIN_LIMIT_DISATNCE_VALUE * 1000.0;

    protected double mLatitude;
    protected double mLongitued;

    protected double mInnerRadius = 0;
    protected double mOuterRadius = 0;
    protected double mScanRadius = 0;
    protected double mSpace = 0;

    protected double mMaxDistance = Double.MIN_VALUE;
    protected double mMinDistance = Double.MAX_VALUE;
    protected double mCurScanDistance = 1;
    protected double mCurOuterDistance = 1;
    protected double mLastScanDistance = 1;

    protected Paint mPaint = new Paint();
    protected Paint mOuterPaint = new Paint();

    protected Map<Long, Point> mPointMap = new HashMap<Long, Point>();
    protected List<NearbyMember> mMembers = new ArrayList<NearbyMember>();

    protected HashSet<Long> mInScanAreaMemberSet = new HashSet<Long>();

    protected HashSet<Long> mIgnoreMemberSet = new HashSet<Long>();
    protected HashSet<Long> mTmpIgnoreMemberSet = new HashSet<Long>();

    protected HashSet<Long> mScanAnimSet = new HashSet<Long>();

    protected int mCicleRadius = 0;
    protected int mExtraClickRadius = 0; // 额外扩大的点击范围

    protected boolean isScaling = false;

    protected int mTouchSlop = 0;

    public RadarMembersView(Context context) {
        super(context);
        init(context);
    }

    public RadarMembersView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RadarMembersView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void init(Context ctx) {
        mCicleRadius = DisplayUtil.dip2px(getContext(), CIRCLE_RADIUS);
        mExtraClickRadius = DisplayUtil.dip2px(getContext(), EXTRA_CLICK_RADIUS);

        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAntiAlias(true);

        mOuterPaint.setStyle(Paint.Style.STROKE);
        mOuterPaint.setAntiAlias(true);
        mOuterPaint.setStrokeWidth(1);
        mOuterPaint.setColor(Color.parseColor("#80ffffff"));
        mOuterPaint.setShadowLayer(2, 1, 1, Color.parseColor("#80ffffff"));

        mHandlerThread = new HandlerThread("RadarMembersView");
        mHandlerThread.start();
        mWorkHandler = new WorkHandler(mHandlerThread.getLooper());

        try {
            ViewConfiguration vc = ViewConfiguration.get(ctx);
            mTouchSlop = vc.getScaledTouchSlop();
        } catch (Exception e) {
        }
    }

    public void setMyselfGps(double latitude, double longitude) {
        this.mLatitude = latitude;
        this.mLongitued = longitude;
    }

    public void setMembers(List<NearbyMember> members) {
        long t1 = System.currentTimeMillis();
        int size = 0;
        synchronized (mMembers) {
            mRadaScanView.reset();

            mWorkHandler.removeMessages(MSG_CALC_HIDE_POINTS);
            mPointMap.clear();
            mMembers.clear();
            mInScanAreaMemberSet.clear();
            mIgnoreMemberSet.clear();
            mTmpIgnoreMemberSet.clear();
            mScanAnimSet.clear();

            mMaxDistance = Double.MIN_VALUE;
            mMinDistance = Double.MAX_VALUE;

            isScaling = false;

            size = members.size();
            for (int i = 0; i < size; i++) {
                NearbyMember member = members.get(i);

                if (member.distance > mMaxDistance) {
                    mMaxDistance = member.distance;
                }
                if (member.distance < mMinDistance) {
                    mMinDistance = member.distance;
                }

                member.degree = getDegree(mLatitude, mLongitued, member.latitude, member.longitude);
                member.radians = Math.toRadians(member.degree);

                this.mMembers.add(member);
            }
            
            //最大距离小于10km时，按10km算
            if (mMaxDistance < ORIGIN_SHOW_RANGE){
                mMaxDistance = ORIGIN_SHOW_RANGE;
            }
            
            mCurScanDistance = mMaxDistance;
            mCurOuterDistance = mCurScanDistance * mOuterRadius / mScanRadius;
        }

        mMemberInitCount.getAndAdd(1);

        long t2 = System.currentTimeMillis();
    }

    public double getDegree(double fromLat, double fromLon, double toLat, double toLon) {
        double fLat = Math.toRadians(fromLat);
        double fLon = Math.toRadians(fromLon);

        double tLat = Math.toRadians(toLat);
        double tLon = Math.toRadians(toLon);

        double degree = Math.toDegrees(Math.atan2(Math.sin(tLon - fLon) * Math.cos(tLat),
                Math.cos(fLat) * Math.sin(tLat) - Math.sin(fLat) * Math.cos(tLat) * Math.cos(tLon - fLon)));

        if (degree >= 0) {
            return -degree;
        } else {
            return -(360 + degree);
        }
    }

    /** 调用setMembers 后，必须调用setShowRange */
    public void setShowRange(double range) {
        if (range < mMinDistance) {
            range = mMinDistance;

        }
        //range要以setShowRanage为准，不应该以mMaxDistance为上限
//        else if (range > mMaxDistance) {
//            range = mMaxDistance;
//        }
        mCurScanDistance = range;
        mCurOuterDistance = mCurScanDistance * mOuterRadius / mScanRadius;

        refreshPoints(mCurScanDistance);
    }

    public double getCurRange() {
        return mCurScanDistance;
    }

    public Point getPointOfMember(long uin) {
        return mPointMap.get(uin);
    }

    public void setRadius(double innerRadius, double outerRadius, double scanRadius, double space) {
        this.mOuterRadius = outerRadius;
        this.mInnerRadius = innerRadius;
        this.mScanRadius = scanRadius;
        this.mSpace = space;
    }

    Rect r1 = new Rect();
    Rect r2 = new Rect();

    protected void calcHidePoints(double curScanDistance) {
        long t1 = System.currentTimeMillis();

        mTmpIgnoreMemberSet.clear();

        int radius = (int) (mCicleRadius - mCicleRadius * 0.5);
        int size = mMembers.size();
        for (int i = 0; i < size; i++) {
            NearbyMember member = mMembers.get(i);
            Point p1 = mPointMap.get(member.uin);
            if (p1 == null) {
                continue;
            }
            if (member.distance > curScanDistance) {
                continue;
            }
            if (mTmpIgnoreMemberSet.contains(member.uin)) {
                continue;
            }
            r1.set(p1.x - radius, p1.y - radius, p1.x + radius, p1.y + radius);

            for (int j = 0; j < size; j++) {
                NearbyMember neighbMember = mMembers.get(j);
                if (member.uin == neighbMember.uin) {
                    continue;
                }
                if (neighbMember.distance > curScanDistance) {
                    continue;
                }
                if (mTmpIgnoreMemberSet.contains(neighbMember.uin)) {
                    continue;
                }

                Point p2 = mPointMap.get(neighbMember.uin);
                if (p2 == null) {
                    continue;
                }

                r2.set(p2.x - radius, p2.y - radius, p2.x + radius, p2.y + radius);
                if (r1.intersect(r2)) {
                    if (member.sex == NearbyMember.SEX_FEMALE) {
                        mTmpIgnoreMemberSet.add(neighbMember.uin);

                    } else if (neighbMember.sex == NearbyMember.SEX_FEMALE) {
                        mTmpIgnoreMemberSet.add(member.uin);
                        break;
                    } else {
                        if (member.distance <= neighbMember.distance) {
                            mTmpIgnoreMemberSet.add(neighbMember.uin);
                        } else {
                            mTmpIgnoreMemberSet.add(member.uin);
                            break;
                        }
                    }
                }
            }
        }

        mIgnoreMemberSet.clear();
        mIgnoreMemberSet.addAll(mTmpIgnoreMemberSet);

        isHidePointsInited = true;

        long t2 = System.currentTimeMillis();
    }

    protected void calcXY(double curScanDistance) {
        if (mSpace == 0) {
            return;
        }
        long t1 = System.currentTimeMillis();

        int size = mMembers.size();
        for (int i = 0; i < size; i++) {
            NearbyMember member = mMembers.get(i);
            Point point = mPointMap.get(member.uin);
            if (point == null) {
                point = new Point();
            }

            double distance = Math.max(mInnerRadius, member.distance / curScanDistance * mScanRadius);
            point.x = (int) (distance * Math.cos(member.radians) + mCenterX);
            point.y = (int) (distance * Math.sin(member.radians) + mCenterY);

            mPointMap.put(member.uin, point);

            if (member.distance > curScanDistance) {
                mInScanAreaMemberSet.remove(member.uin);
            } else {
                mInScanAreaMemberSet.add(member.uin);
            }
        }

        long t2 = System.currentTimeMillis();
    }

    protected HandlerThread mHandlerThread;
    protected WorkHandler mWorkHandler;

    protected static final int MSG_CALC_HIDE_POINTS = 1;

    public void refreshPoints(double curScanDistance) {
        calcXY(curScanDistance);

        mWorkHandler.removeMessages(MSG_CALC_HIDE_POINTS);
        Message msg = Message.obtain();
        msg.what = MSG_CALC_HIDE_POINTS;
        msg.obj = curScanDistance;
        mWorkHandler.sendMessageDelayed(msg, 15);

        invalidate();
    }

    protected class WorkHandler extends Handler {
        public WorkHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(Message msg) {
            double curScanDistance = (Double) msg.obj;
            synchronized (mMembers) {
                calcHidePoints(curScanDistance);
            }
            postInvalidate();
        }
    };

    public void destroy() {
        mWorkHandler.removeCallbacksAndMessages(null);
        mHandlerThread.quit();
    }

    protected int mCenterX;
    protected int mCenterY;
    protected int mWidth;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        mCenterX = getWidth() / 2;
        mCenterY = mCenterX;
        mWidth = getWidth();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long t1 = System.currentTimeMillis();
        int firstSelectedMemPos = -1;
        int size = mMembers.size();
        for (int i = 0; i < size; i++) {
            NearbyMember member = mMembers.get(i);
            long uin = member.uin;

            int alpha = 0xff;
            mOuterPaint.setAlpha(0x80);

            if (!isInFirstScan) {
                continue;
            }

            Point point = mPointMap.get(uin);
            if (point == null) {
                continue;
            }
            if (member.distance > mCurOuterDistance) {
                continue;
            } else if (member.distance > mCurScanDistance) {
                if (!isScaling) {
                    continue;
                }
                double factor = (member.distance - mCurScanDistance) / (mCurOuterDistance - mCurScanDistance);
                alpha = (int) (0xf0 * (1 - factor * 2));
                if (alpha < 0) {
                    alpha = 0;
                }
                mOuterPaint.setAlpha(alpha);
                if (firstSelectedMemPos == -1) {
                    firstSelectedMemPos = i;
                }
            }

            if (mIgnoreMemberSet.contains(uin)) {
                // mPaint.setAlpha(10);
                continue;
            }

            if (mMemberInitCount.get() == 1 && !isScanRepeated) {
                boolean needShow = false;

                if (mScanAnimSet.contains(member.uin)) {
                    needShow = true;

                } else {
                    double degree = getDegree(point.x - mCenterX, point.y - mCenterX);
                    double curScanDegree = mCurentScanAreaDegree;
                    double range = curScanDegree - 10;

                    if (range >= 0) {
                        needShow = degree >= range && degree <= curScanDegree;
                    } else {
                        needShow = (degree >= 0 && degree <= curScanDegree)
                                || (degree >= (360 - (15 - curScanDegree)) && degree <= 360);
                    }
                }

                if (!needShow) {
                    continue;
                } else {
                    mScanAnimSet.add(member.uin);
                }
            }

            if (member.sex == NearbyMember.SEX_FEMALE) {
                mPaint.setColor(0xffff90a2);
            } else {
                mPaint.setColor(0xff43d5ff);
            }
            mPaint.setAlpha(alpha);

            canvas.drawCircle(point.x, point.y, mCicleRadius, mPaint);
            canvas.drawCircle(point.x, point.y, mCicleRadius, mOuterPaint);
        }

        if (mOnDrawListener != null && size > 0) {
            mOnDrawListener.onReady(mMembers.get(firstSelectedMemPos == -1 ? 0 : firstSelectedMemPos).uin);
        }
    }

    /** 以圆心为原点，计算夹角 */
    public static double getDegree(int x, int y) {
        if (x == 0) {
            if (y <= 0) {
                return 270;
            } else {
                return 90;
            }
        }

        if (y == 0) {
            if (x >= 0) {
                return 0;
            } else {
                return 180;
            }
        }

        double degree = Math.toDegrees(Math.atan(Math.abs(y * 1.0 / x)));
        if (x > 0 && y > 0) {
            return degree;
        } else if (x < 0 && y > 0) {
            return 180 - degree;
        } else if (x < 0 && y < 0) {
            return 180 + degree;
        } else if (x > 0 && y < 0) {
            return 360 - degree;
        }

        return degree;
    }

    protected double mInitDelta = 0f;
    protected double mLastDelta = 0f;

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (mMembers.size() == 0 || (isInFirstScan && !isScanRepeated)) {
            return true;
        }
        int pointCount = event.getPointerCount();

        if (pointCount == 1) {
            return mDetector.onTouchEvent(event);

        } else if (pointCount >= 2) {
            int action = event.getAction() & MotionEvent.ACTION_MASK;
            int dx = (int) event.getX(0) - (int) event.getX(1);
            int dy = (int) event.getY(0) - (int) event.getY(1);

            switch (action) {
            case MotionEvent.ACTION_POINTER_DOWN:
                mInitDelta = Math.sqrt(dx * dx + dy * dy);
                mLastDelta = mInitDelta;
                mLastScanDistance = mCurScanDistance;

                if (mOnScaleListener != null) {
                    mOnScaleListener.onScaleStart(getCurRange(), mInScanAreaMemberSet);
                }
                if (mRadaScanView != null) {
                    mRadaScanView.onScaleChange(isScaling, mInScanAreaMemberSet, mPointMap);
                }

                if (mRadarBgView != null) {
                    mRadarBgView.onScaleChange(action, (int) mInitDelta);
                }

                invalidate();

                break;
            case MotionEvent.ACTION_MOVE:
                int dis = (int) (Math.sqrt(dx * dx + dy * dy));
                if (Math.abs(dis - mLastDelta) < mTouchSlop) {
                    break;
                }
                isScaling = true;

                if (mInitDelta != 0) {
                    double factor = dis / mInitDelta;

                    mCurScanDistance = mLastScanDistance / factor;

                    if (mCurScanDistance < mMinDistance) {
                        mCurScanDistance = mMinDistance;

                    } else if (mCurScanDistance > mMaxDistance) {
                        mCurScanDistance = mMaxDistance;
                    }
                    mCurOuterDistance = mCurScanDistance * mOuterRadius / mScanRadius;

                    if (mRadarBgView != null) {
                        if (dis > mLastDelta && mCurScanDistance == mMinDistance) {
                            // 放大

                        } else if (dis < mLastDelta && mCurScanDistance == mMaxDistance) {
                            // 缩小

                        } else {
                            mRadarBgView.onScaleChange(action, (int) dis);
                        }
                    }
                }

                mLastDelta = dis;

                refreshPoints(mCurScanDistance); // 在下面之前调用

                if (mOnScaleListener != null) {
                    mOnScaleListener.onScaleing(getCurRange(), mInScanAreaMemberSet);
                }
                if (mRadaScanView != null) {
                    mRadaScanView.onScaleChange(isScaling, mInScanAreaMemberSet, mPointMap);
                }

                break;

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_POINTER_UP:
                isScaling = false;

                if (mRadarBgView != null) {
                    mRadarBgView.onScaleChange(action, 0);
                }

                refreshPoints(mCurScanDistance);// 在下面之前调用

                if (mOnScaleListener != null) {
                    mOnScaleListener.onScaleFinish(getCurRange(), mInScanAreaMemberSet);
                }
                if (mRadaScanView != null) {
                    mRadaScanView.onScaleChange(isScaling, mInScanAreaMemberSet, mPointMap);
                }

                break;

            default:
                break;
            }

            return true;
        }
        return true;
    }

    protected long mSelectionUin = 0;

    public void setSelection(long uin) {
        if (mRadaScanView != null) {
            mSelectionUin = uin;
            mRadaScanView.setSelection(uin, mInScanAreaMemberSet, mPointMap);
        }
    }

    @SuppressWarnings("deprecation")
    private GestureDetector mDetector = new GestureDetector(new OnGestureListener() {

        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            int x = (int) e.getX();
            int y = (int) e.getY();

            long t1 = System.currentTimeMillis();
            String memberUin = "";
            int radius = mCicleRadius + mExtraClickRadius;

            for (NearbyMember member : mMembers) {
                Point p = getPointOfMember(member.uin);
                if (member.distance > mCurScanDistance) {
                    continue;
                }

                if (x > p.x - radius && x < p.x + radius && y > p.y - radius && y < p.y + radius) {
                    memberUin = member.uin + "";
                    if (mOnIconClickListener != null) {
                        mOnIconClickListener.onIconClick(member.uin);
                    }
                    break;
                }
            }
            long t2 = System.currentTimeMillis();

            return true;
        }

        @Override
        public void onShowPress(MotionEvent e) {

        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            return true;
        }

        @Override
        public void onLongPress(MotionEvent e) {

        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            return true;
        }

        @Override
        public boolean onDown(MotionEvent e) {
            return true;
        }
    });

    public OnMemberIconClickListener mOnIconClickListener;

    public void setOnMemberIconClickListener(OnMemberIconClickListener listener) {
        mOnIconClickListener = listener;
    }

    public OnScaleListener mOnScaleListener;

    public void setOnScaleListener(OnScaleListener listener) {
        mOnScaleListener = listener;
    }

    public static interface OnMemberIconClickListener {
        public void onIconClick(long uin);
    }

    public static interface OnScaleListener {
        public void onScaleStart(double curRange, HashSet<Long> inScanAreaMemberSet);

        public void onScaleing(double curRange, HashSet<Long> inScanAreaMemberSet);

        public void onScaleFinish(double curRange, HashSet<Long> inScanAreaMemberSet);
    }

    protected OnDrawReadyListener mOnDrawListener;

    public static interface OnDrawReadyListener {
        void onReady(long firstSelectedUin);
    }

    public void setOnDrawReadyListener(OnDrawReadyListener listener) {
        mOnDrawListener = listener;
    }

    public void removeOnDrawListener() {
        mOnDrawListener = null;
    }

    RadaScanView mRadaScanView;

    public void setRadaScanView(RadaScanView radaScanView) {
        mRadaScanView = radaScanView;
    }

    RadarBgView mRadarBgView;

    public void setRadarBgView(RadarBgView radarBgView) {
        mRadarBgView = radarBgView;
    }

    protected volatile double mInitScanDegree = 0;
    protected volatile double mCurentScanAreaDegree = -1;
    protected volatile boolean isScanRepeated = false;
    protected volatile boolean isInFirstScan = false;
    protected volatile boolean isHidePointsInited = false;

    protected AtomicInteger mMemberInitCount = new AtomicInteger(0);

    /** 非UI线程 */
    public void onScanAreaChange(double degree) {
        if (mMemberInitCount.get() != 1 || isScanRepeated || !isHidePointsInited) {
            return;
        }

        if (mCurentScanAreaDegree < 0) {
            isInFirstScan = true;
            mInitScanDegree = degree;

        } else if (degree - mInitScanDegree > 360) {
            isScanRepeated = true;

            if (mOnFirstScanFinishListener != null) {
                mOnFirstScanFinishListener.onFirstScanFinish();
            }
        }

        if (isInFirstScan && !isScanRepeated) {
            mCurentScanAreaDegree = degree % 360;

            postInvalidate();
        }
    }

    protected OnFirstScanFinishListener mOnFirstScanFinishListener;

    public void setOnFirstScanFinishListener(OnFirstScanFinishListener listener) {
        mOnFirstScanFinishListener = listener;
    }

    public static interface OnFirstScanFinishListener {
        public void onFirstScanFinish();
    }
}
