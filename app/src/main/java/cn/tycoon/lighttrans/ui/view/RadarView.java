package cn.tycoon.lighttrans.ui.view;

import java.lang.reflect.Field;
import java.util.List;

import android.content.Context;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.animation.AlphaAnimation;
import android.widget.RelativeLayout;

import cn.tycoon.lighttrans.R;
import cn.tycoon.lighttrans.data.NearbyMember;
import cn.tycoon.lighttrans.utils.DisplayUtil;

/**
 * 整个雷达的View，包括了一个背景（圈圈和扫描，所有成员标记View，中心头像View和两个头像图片）
 * 
 * @author sidwang
 */
public class RadarView extends RelativeLayout {
    protected final String TAG = "RadarView";

    protected RadarBgView mRadarBgView;
    protected RadarMembersView mRadarMembersView;
    protected RadaScanView mRadaScanView;

    protected Context mContext;

    protected RadarBgView.OnBackgroundReadyListener mListener;

    public RadarView(Context context) {
        super(context);
        init(context);
    }

    public RadarView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public RadarView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    public void setApp() {
        mRadaScanView.setApp();
    }

    private void init(Context ctx) {
        mContext = ctx;
        initMearsure();

        LayoutInflater inflater = LayoutInflater.from(getContext());
        inflater.inflate(R.layout.radar_view, this);
        mRadarBgView = (RadarBgView) findViewById(R.id.radar_bg_view);
        mRadarMembersView = (RadarMembersView) findViewById(R.id.radar_member_view);
        mRadaScanView = (RadaScanView) findViewById(R.id.radar_scan_view);
        mRadarMembersView.setRadarBgView(mRadarBgView);
        mRadarMembersView.setRadaScanView(mRadaScanView);

        mRadarBgView.setOnBackgroundReadyListenerr(new RadarBgView.OnBackgroundReadyListener() {
            @Override
            public void onReady(int padding, int centerImgSize, int innerRadius, int outerRadius, int scanRadius,
                    int space) {
                mRadarMembersView.setRadius(innerRadius, outerRadius, scanRadius, space);
                mRadarMembersView.invalidate();

                if (mListener != null) {
                    mListener.onReady(padding, centerImgSize, innerRadius, outerRadius, scanRadius, space);
                }

                mRadaScanView.setSpace(padding, space, scanRadius);
                mRadaScanView.setOnScanDegreeChangeListener(new RadaScanView.OnScanDegreeChangeListener() {
                    @Override
                    public void onDegreeChage(float degree) {
                        mRadarMembersView.onScanAreaChange(degree);
                    }
                });
            }
        });
    }

    public void removeRadarMembersViewOnDrawListener() {
        mRadarMembersView.removeOnDrawListener();
    }

    public void setmRadarMembersViewOnDrawListener(RadarMembersView.OnDrawReadyListener onDrawListener) {
        mRadarMembersView.setOnDrawReadyListener(onDrawListener);
    }

    public void setOnBackgroundReadyListener(RadarBgView.OnBackgroundReadyListener mListener) {
        this.mListener = mListener;
    }

    public void setSelection(long uin) {
        mRadarMembersView.setSelection(uin);
    }

    public void setNearbyMembers(List<NearbyMember> members) {
        mRadarMembersView.setMembers(members);
        AlphaAnimation animation = new AlphaAnimation(0.0f, 1.0f);
        animation.setDuration(500);
        mRadarMembersView.startAnimation(animation);
    }

    public void setMyselfGps(double latitude, double longitude) {
        mRadarMembersView.setMyselfGps(latitude, longitude);
    }

    public int getBarHeight() {
        Class<?> c = null;
        Field field = null;
        int x = 0;
        int sbar = 0;
        try {
            c = Class.forName("com.android.internal.R$dimen");
            field = c.getField("status_bar_height");
            x = (Integer) field.get(null);
            sbar = getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (sbar == 0) {
            sbar = DisplayUtil.dip2px(mContext, 26);
        }

        return sbar;
    }

    // protected int mLeftHeightForRadar = 0;
    // protected int mStatusBarHeight = 0;
    protected DisplayMetrics mDisplayMetrics;

    protected void initMearsure() {
        mDisplayMetrics = getResources().getDisplayMetrics();
        // mStatusBarHeight = getBarHeight();

        // int cardHeight =
        // getResources().getDimensionPixelSize(R.dimen.qq_troop_nearby_member_card_h);
        // int cardBottomMargin =
        // getResources().getDimensionPixelSize(R.dimen.qq_troop_nearby_member_card_margin_bottom);
        // int titleHeight =
        // getResources().getDimensionPixelSize(R.dimen.title_bar_height);

        // mLeftHeightForRadar = mDisplayMetrics.heightPixels - mStatusBarHeight
        // - titleHeight - cardHeight
        // - cardBottomMargin + DisplayUtil.dip2px(getContext(),
        // RadarBgView.MARGIN - 2);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        int spec = widthMeasureSpec;
        if (height < width) {
            spec = heightMeasureSpec;
        }

        // if (mLeftHeightForRadar < mDisplayMetrics.widthPixels &&
        // mLeftHeightForRadar > 0) {
        // widthMeasureSpec =
        // MeasureSpec.makeMeasureSpec(mLeftHeightForRadar,
        // MeasureSpec.EXACTLY);
        // }
        
        
        // 确保是方形的
        super.onMeasure(spec, spec);
    }

    public void setOnMemberIconClickListener(RadarMembersView.OnMemberIconClickListener listener) {
        if (mRadarMembersView != null) {
            mRadarMembersView.setOnMemberIconClickListener(listener);
        }
    }

    public void setOnScaleListener(RadarMembersView.OnScaleListener listener) {
        if (mRadarMembersView != null) {
            mRadarMembersView.setOnScaleListener(listener);
        }
    }

    public void setShowRange(double range) {
        if (mRadarMembersView != null) {
            mRadarMembersView.setShowRange(range);
        }
    }

    public double getCurRange() {
        if (mRadarMembersView != null) {
            return mRadarMembersView.getCurRange();
        }
        return 1; // 见mCurScanDistance的初始值
    }

    public void setOnFirstScanFinishListener(RadarMembersView.OnFirstScanFinishListener listener) {
        if (mRadarMembersView != null) {
            mRadarMembersView.setOnFirstScanFinishListener(listener);
        }
    }

    public void setStartScan(boolean startScan) {
        if (mRadaScanView != null) {
            if (startScan) {
                mRadaScanView.startDraw();
            } else {
                mRadaScanView.stopDraw();
            }
        }
    }

    public void destroy() {
        if (mRadarMembersView != null) {
            mRadarMembersView.destroy();
        }

        if (mRadaScanView != null) {
            mRadaScanView.destroy();
        }
    }

}
