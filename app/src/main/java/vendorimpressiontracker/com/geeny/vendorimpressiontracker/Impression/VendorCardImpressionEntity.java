package vendorimpressiontracker.com.geeny.vendorimpressiontracker.Impression;

public class VendorCardImpressionEntity implements ImpressionInterface {

    protected static final int MIN_VIEWED_HEIGHT_PERCENT = 50;
    protected static final int MIN_VIEWED_WIDTH_PERCENT = 100;
    private static final int DELAY_MILLIS = 1000;
    private ImpressionTracker.OnTrackImpressionListener mOnTrackImpressionListener;
    private boolean mImpressionRecorded;

    public VendorCardImpressionEntity(ImpressionTracker.OnTrackImpressionListener onTrackImpressionListener) {
        this.mOnTrackImpressionListener = onTrackImpressionListener;
    }

    @Override
    public int getImpressionHeightViewedPercentage() {
        return MIN_VIEWED_HEIGHT_PERCENT;
    }

    @Override
    public int getImpressionWidthViewedPercentage() {
        return MIN_VIEWED_WIDTH_PERCENT;
    }

    @Override
    public Integer getImpressionMinVisiblePx() {
        return null;
    }

    @Override
    public int getImpressionMinTimeViewed() {
        return DELAY_MILLIS;
    }

    @Override
    public void trackImpression() {
        if (mOnTrackImpressionListener != null) {
            mOnTrackImpressionListener.trackImpressionEvent();
        }
    }

    @Override
    public boolean isImpressionRecorded() {
        return mImpressionRecorded;
    }

    @Override
    public void setImpressionRecorded() {
        mImpressionRecorded = true;
    }
}
