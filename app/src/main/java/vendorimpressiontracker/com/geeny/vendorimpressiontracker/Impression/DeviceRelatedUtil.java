package vendorimpressiontracker.com.geeny.vendorimpressiontracker.Impression;

import android.content.Context;
import android.util.DisplayMetrics;

/**
 * Created by john on 2016/12/27.
 */

public class DeviceRelatedUtil {


    private static DeviceRelatedUtil sInstance;
    private Context mContext;
    private DisplayMetrics mDisplayMetrics;

    private DeviceRelatedUtil(Context context) {
        this.mContext = context;
        mDisplayMetrics = this.mContext.getResources().getDisplayMetrics();
    }

    public static DeviceRelatedUtil getInstance() {
        return sInstance;
    }

    public static void init(Context context) {
        if (sInstance == null) {
            sInstance = new DeviceRelatedUtil(context);
        }
    }

    public float getDensity() {
        return mDisplayMetrics.density;
    }

    public int getDisplayHeight() {
        return mDisplayMetrics.heightPixels;
    }

    public int getDisplayWidth() {
        return mDisplayMetrics.widthPixels;
    }
}
