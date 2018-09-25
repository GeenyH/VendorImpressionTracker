package vendorimpressiontracker.com.geeny.vendorimpressiontracker.Impression;

import android.content.Context;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

public class VisibilityTracker {

    static final int NUM_ACCESSES_BEFORE_TRIMMING = 50;
    private static final int VISIBILITY_THROTTLE_MILLIS = 100;
    @NonNull
    final ViewTreeObserver.OnPreDrawListener mOnPreDrawListener;
    @NonNull
    private final ArrayList<View> mTrimmedViews;
    @NonNull
    private final Map<View, TrackingInfo> mTrackedViews;
    @NonNull
    private final VisibilityChecker mVisibilityChecker;
    @NonNull
    private final VisibilityRunnable mVisibilityRunnable;
    @NonNull
    private final Handler mVisibilityHandler;
    @NonNull
    WeakReference<ViewTreeObserver> mWeakViewTreeObserver;
    private long mAccessCounter = 0;
    @Nullable
    private VisibilityTrackerListener mVisibilityTrackerListener;
    private boolean mIsVisibilityScheduled;

    public VisibilityTracker(@NonNull final Context context) {
        this(context,
                new WeakHashMap<>(10),
                new VisibilityChecker(),
                new Handler());
    }

    VisibilityTracker(@NonNull final Context context,
                      @NonNull final Map<View, TrackingInfo> trackedViews,
                      @NonNull final VisibilityChecker visibilityChecker,
                      @NonNull final Handler visibilityHandler) {
        mTrackedViews = trackedViews;
        mVisibilityChecker = visibilityChecker;
        mVisibilityHandler = visibilityHandler;
        mVisibilityRunnable = new VisibilityRunnable();
        mTrimmedViews = new ArrayList<>(NUM_ACCESSES_BEFORE_TRIMMING);


        mOnPreDrawListener = () -> {
            scheduleVisibilityCheck();
            return true;
        };

        mWeakViewTreeObserver = new WeakReference<>(null);
        setViewTreeObserver(context, null);
    }

    /**
     * 对view的最顶层布局进行监听和设置回调，一旦整个布局出现重新绘画，马上回调（开启轮询操作）
     *
     * @param context
     * @param view
     */
    private void setViewTreeObserver(@Nullable final Context context, @Nullable final View view) {
        final ViewTreeObserver originalViewTreeObserver = mWeakViewTreeObserver.get();
        if (originalViewTreeObserver != null && originalViewTreeObserver.isAlive()) {
            return;
        }

        final View rootView = Views.getTopmostView(context, view);
        if (rootView == null) {
            Log.d("mopub", "Unable to set Visibility Tracker due to no available root view.");
            return;
        }

        final ViewTreeObserver viewTreeObserver = rootView.getViewTreeObserver();
        if (!viewTreeObserver.isAlive()) {
            Log.w("mopub", "Visibility Tracker was unable to track views because the"
                    + " root view tree observer was not alive");
            return;
        }

        mWeakViewTreeObserver = new WeakReference<>(viewTreeObserver);
        viewTreeObserver.addOnPreDrawListener(mOnPreDrawListener);
    }

    void setVisibilityTrackerListener(
            @Nullable final VisibilityTrackerListener visibilityTrackerListener) {
        mVisibilityTrackerListener = visibilityTrackerListener;
    }

    void addView(@NonNull final View view,
                 @Nullable final Integer minVisiblePx, final int minHeight, final int minWidth) {
        addView(view, view, minVisiblePx, minHeight, minWidth);
    }

    void addView(@NonNull View rootView, @NonNull final View view,
                 @Nullable final Integer minVisiblePx, final int minHeight, final int minWidth) {
        setViewTreeObserver(view.getContext(), view);

        TrackingInfo trackingInfo = mTrackedViews.get(view);
        if (trackingInfo == null) {
            trackingInfo = new TrackingInfo();
            mTrackedViews.put(view, trackingInfo);
            scheduleVisibilityCheck();
        }

        trackingInfo.mRootView = rootView;
        trackingInfo.mAccessOrder = mAccessCounter;
        trackingInfo.mMinVisiblePx = minVisiblePx;
        trackingInfo.mHeightViewedPercentage = minHeight;
        trackingInfo.mWidthViewedPercentage = minWidth;

        mAccessCounter++;
        if (mAccessCounter % NUM_ACCESSES_BEFORE_TRIMMING == 0) {
            trimTrackedViews(mAccessCounter - NUM_ACCESSES_BEFORE_TRIMMING);
        }
    }

    /*
    mTrackedViews中，TrackingInfo里面对应的序号，存储的view过多时，我们选择去掉较前的view
    方法根据最小的序号排除掉小于这个序号的之前的view
     */
    private void trimTrackedViews(long minAccessOrder) {
        for (final Map.Entry<View, TrackingInfo> entry : mTrackedViews.entrySet()) {
            if (entry.getValue().mAccessOrder < minAccessOrder) {
                mTrimmedViews.add(entry.getKey());
            }
        }

        for (View view : mTrimmedViews) {
            removeView(view);
        }
        mTrimmedViews.clear();
    }

    void removeView(@NonNull final View view) {
        mTrackedViews.remove(view);
    }

    void clear() {
        mTrackedViews.clear();
        mVisibilityHandler.removeMessages(0);
        mIsVisibilityScheduled = false;
    }

    void destroy() {
        clear();
        final ViewTreeObserver viewTreeObserver = mWeakViewTreeObserver.get();
        if (viewTreeObserver != null && viewTreeObserver.isAlive()) {
            viewTreeObserver.removeOnPreDrawListener(mOnPreDrawListener);
        }
        mWeakViewTreeObserver.clear();
        mVisibilityTrackerListener = null;
    }

    void scheduleVisibilityCheck() {
        if (mIsVisibilityScheduled) {
            return;
        }

        mIsVisibilityScheduled = true;
        mVisibilityHandler.postDelayed(mVisibilityRunnable, VISIBILITY_THROTTLE_MILLIS);
    }

    interface VisibilityTrackerListener {
        void onVisibilityChanged(List<View> visibleViews, List<View> invisibleViews);
    }

    static class TrackingInfo {
        int mHeightViewedPercentage;
        int mWidthViewedPercentage;

        long mAccessOrder;
        View mRootView;

        @Nullable
        Integer mMinVisiblePx;
    }

    static class VisibilityChecker {
        private final Rect mClipRect = new Rect();

        /**
         * 传入View开始出现在屏幕的时间和曝光时间阈值，判断一个View是否满足记录印象行为的条件
         *
         * @param startTimeMillis
         * @param minTimeViewed
         * @return
         */
        boolean hasRequiredTimeElapsed(final long startTimeMillis, final int minTimeViewed) {
            return SystemClock.uptimeMillis() - startTimeMillis >= minTimeViewed;
        }

        /**
         * 判断一个View是否满足可见条件
         *
         * @param rootView
         * @param view
         * @param minVisiblePx    最小的可见像素个数，这里暂时不用
         * @param minHeightViewed 最小的高度
         * @param minWidthViewed  最小的宽度
         * @return
         */
        boolean isVisible(@Nullable final View rootView, @Nullable final View view,
                          @Nullable final Integer minVisiblePx, int minHeightViewed, int minWidthViewed) {
            if (view == null || view.getVisibility() != View.VISIBLE || rootView.getParent() == null) {
                return false;
            }

            if (!view.getGlobalVisibleRect(mClipRect)) {
                return false;
            }

            int[] location = new int[2];
            view.getLocationOnScreen(location);
            int x1 = location[0];
            int y1 = location[1];
            int x2 = x1 + view.getWidth();
            int y2 = y1 + view.getHeight();

            if ((x1 == 0 && y1 == 0) || x1 > DeviceRelatedUtil.getInstance().getDisplayWidth()
                    || x2 < 0 || y1 > DeviceRelatedUtil.getInstance().getDisplayHeight() || y2 < 0) {
                return false;
            }

            final int visibleViewHeight = mClipRect.height() * 100;
            final int visibleViewWidth = mClipRect.width() * 100;

            final int totalViewHeight = view.getHeight() * minHeightViewed;
            final int totalViewWidth = view.getWidth() * minWidthViewed;

            return visibleViewHeight >= totalViewHeight && visibleViewWidth >= totalViewWidth;
        }
    }

    class VisibilityRunnable implements Runnable {
        @NonNull
        private final ArrayList<View> mVisibleViews;
        @NonNull
        private final ArrayList<View> mInvisibleViews;

        VisibilityRunnable() {
            mInvisibleViews = new ArrayList<>();
            mVisibleViews = new ArrayList<>();
        }

        /*
        一个线程，里面对mTrackedViews进行遍历
         */
        @Override
        public void run() {
            mIsVisibilityScheduled = false;
            for (final Map.Entry<View, TrackingInfo> entry : mTrackedViews.entrySet()) {
                final View view = entry.getKey();
                final int minHeightViewed = entry.getValue().mHeightViewedPercentage;
                final int minWidthViewed = entry.getValue().mWidthViewedPercentage;
                final Integer minVisiblePx = entry.getValue().mMinVisiblePx;
                final View rootView = entry.getValue().mRootView;


                if (mVisibilityChecker.isVisible(rootView, view, minVisiblePx, minHeightViewed, minWidthViewed)) {
                    mVisibleViews.add(view);
                } else if (!mVisibilityChecker.isVisible(rootView, view, null, minHeightViewed, minWidthViewed)) {
                    mInvisibleViews.add(view);
                }
            }


            if (mVisibilityTrackerListener != null) {
                mVisibilityTrackerListener.onVisibilityChanged(mVisibleViews, mInvisibleViews);
            }

            mVisibleViews.clear();
            mInvisibleViews.clear();
        }
    }
}
