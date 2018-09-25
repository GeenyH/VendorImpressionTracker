package vendorimpressiontracker.com.geeny.vendorimpressiontracker.Impression;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.View;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Map;
import java.util.WeakHashMap;

public class ImpressionTracker {

    private static final int PERIOD = 250;

    @NonNull
    private final VisibilityTracker mVisibilityTracker;

    @NonNull
    private final Map<View, TimestampWrapper<ImpressionInterface>> mPollingViews;  // 存View和时间包装器（存入了View注册的时间和ImpressionInterface回调）

    @NonNull
    private final Map<View, ImpressionInterface> mTrackedViews;  // 存view和回调接口，这个变量只是用于判断View有没有对应的ImpressionInterface


    @NonNull
    private final Handler mPollHandler;

    @NonNull
    private final PollingRunnable mPollingRunnable;

    @NonNull
    private final VisibilityTracker.VisibilityChecker mVisibilityChecker;

    @Nullable
    private VisibilityTracker.VisibilityTrackerListener mVisibilityTrackerListener;

    private LinkedList<String> mImpressionTracked;  // 存入业务信息的ID，比如vendor id

    public ImpressionTracker(@NonNull final Context context) {
        this(new WeakHashMap<>(),
                new WeakHashMap<>(),
                new VisibilityTracker.VisibilityChecker(),
                new VisibilityTracker(context),
                new Handler(Looper.getMainLooper()));
        mImpressionTracked = new LinkedList<>();
    }

    ImpressionTracker(@NonNull final Map<View, ImpressionInterface> trackedViews,
                      @NonNull final Map<View, TimestampWrapper<ImpressionInterface>> pollingViews,
                      @NonNull final VisibilityTracker.VisibilityChecker visibilityChecker,
                      @NonNull final VisibilityTracker visibilityTracker,
                      @NonNull final Handler handler) {
        mTrackedViews = trackedViews;
        mPollingViews = pollingViews;
        mVisibilityChecker = visibilityChecker;
        mVisibilityTracker = visibilityTracker;

        mVisibilityTrackerListener = (visibleViews, invisibleViews) -> {
            for (final View view : visibleViews) {  // 遍历可见的View，插入或更新
                final ImpressionInterface impressionInterface = mTrackedViews.get(view);
                if (impressionInterface == null) {  // 当时add时，View没有存入的ImpressionInterface为空，这个View不需要追踪或无法追踪
                    removeView(view);  // 移除View
                    continue;
                }

                final TimestampWrapper<ImpressionInterface> polling = mPollingViews.get(view);
                if (polling != null && impressionInterface == polling.mInstance) {
                    continue;  // 当前的View已经更新
                }

                // 这个View还没有加入mPollingViews，则加入
                mPollingViews.put(view, new TimestampWrapper<>(impressionInterface));
            }

            for (final View view : invisibleViews) {
                mPollingViews.remove(view);  // 移除View
            }
            scheduleNextPoll();  // 开始轮询
        };
        mVisibilityTracker.setVisibilityTrackerListener(mVisibilityTrackerListener);

        mPollHandler = handler;
        mPollingRunnable = new PollingRunnable();
    }

    public void addView(String vendorId, final View view, @NonNull final ImpressionInterface impressionInterface) {
        if (!mImpressionTracked.contains(vendorId)) {
            mImpressionTracked.add(vendorId);
            if (mTrackedViews.get(view) != null && mTrackedViews.get(view) == impressionInterface) {
                return;
            }

            removeView(view);

            if (impressionInterface.isImpressionRecorded()) {
                return;
            }
            mTrackedViews.put(view, impressionInterface);
            mVisibilityTracker.addView(view, impressionInterface.getImpressionMinVisiblePx(),
                    impressionInterface.getImpressionHeightViewedPercentage(), impressionInterface.getImpressionWidthViewedPercentage());
        }
    }

    public void removeView(final View view) {
        mTrackedViews.remove(view);
        removePollingView(view);
        mVisibilityTracker.removeView(view);
    }

    public void destroy() {
        clear();
        mVisibilityTracker.destroy();
        mVisibilityTrackerListener = null;
    }

    public void clear() {
        mTrackedViews.clear();
        mPollingViews.clear();
        mVisibilityTracker.clear();
        mPollHandler.removeMessages(0);
        mImpressionTracked.clear();
    }


    void scheduleNextPoll() {
        if (mPollHandler.hasMessages(0)) {
            return;
        }

        mPollHandler.postDelayed(mPollingRunnable, PERIOD);
    }

    private void removePollingView(final View view) {
        mPollingViews.remove(view);
    }

    @Nullable
    @Deprecated
    VisibilityTracker.VisibilityTrackerListener getVisibilityTrackerListener() {
        return mVisibilityTrackerListener;
    }

    public ImpressionInterface getVendorCardImpression(OnTrackImpressionListener onTrackImpressionListener) {
        return new VendorCardImpressionEntity(onTrackImpressionListener);
    }

    public interface OnTrackImpressionListener {
        void trackImpressionEvent();
    }

    class PollingRunnable implements Runnable {
        @NonNull
        private final ArrayList<View> mRemovedViews;

        PollingRunnable() {
            mRemovedViews = new ArrayList<>();
        }

        @Override
        public void run() {
            for (final Map.Entry<View, TimestampWrapper<ImpressionInterface>> entry : mPollingViews.entrySet()) {
                final View view = entry.getKey();
                final TimestampWrapper<ImpressionInterface> timestampWrapper = entry.getValue();

                if (!mVisibilityChecker.hasRequiredTimeElapsed(
                        timestampWrapper.mCreatedTimestamp,
                        timestampWrapper.mInstance.getImpressionMinTimeViewed())) {
                    continue;
                }

                // 满足曝光时间后，追踪行为（我们的终极目的）
                timestampWrapper.mInstance.trackImpression();
                timestampWrapper.mInstance.setImpressionRecorded();

                mRemovedViews.add(view);
            }

            for (View view : mRemovedViews) {
                removeView(view);  // 删除已经被追踪的View相关的记录
            }
            mRemovedViews.clear();

            if (!mPollingViews.isEmpty()) {
                scheduleNextPoll();  // 仍然存在注册的View，继续循环监听
            }
        }
    }
}
