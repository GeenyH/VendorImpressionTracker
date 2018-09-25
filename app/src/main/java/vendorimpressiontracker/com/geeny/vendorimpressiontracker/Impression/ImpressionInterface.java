package vendorimpressiontracker.com.geeny.vendorimpressiontracker.Impression;

public interface ImpressionInterface {
    int getImpressionHeightViewedPercentage();

    int getImpressionWidthViewedPercentage();

    Integer getImpressionMinVisiblePx();

    int getImpressionMinTimeViewed();

    void trackImpression();

    boolean isImpressionRecorded();

    void setImpressionRecorded();

}
