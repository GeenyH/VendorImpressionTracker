package vendorimpressiontracker.com.geeny.vendorimpressiontracker.Impression;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.util.Log;
import android.view.View;

public class Views {

    /**
     * Finds the topmost view in the current Activity or current view hierarchy.
     *
     * @param context If an Activity Context, used to obtain the Activity's DecorView. This is
     *                ignored if it is a non-Activity Context.
     * @param view    A View in the currently displayed view hierarchy. If a null or non-Activity
     *                Context is provided, this View's topmost parent is used to determine the
     *                rootView.
     * @return The topmost View in the currency Activity or current view hierarchy. Null if no
     * applicable View can be found.
     */
    @Nullable
    public static View getTopmostView(@Nullable final Context context, @Nullable final View view) {
        final View rootViewFromActivity = getRootViewFromActivity(context);
        final View rootViewFromView = getRootViewFromView(view);

        return rootViewFromActivity != null
                ? rootViewFromActivity
                : rootViewFromView;
    }

    @Nullable
    private static View getRootViewFromActivity(@Nullable final Context context) {
        if (!(context instanceof Activity)) {
            return null;
        }

        return ((Activity) context).getWindow().getDecorView().findViewById(android.R.id.content);
    }

    @Nullable
    private static View getRootViewFromView(@Nullable final View view) {
        if (view == null) {
            return null;
        }

        if (!ViewCompat.isAttachedToWindow(view)) {
            Log.d("rootView", "Attempting to call View#getRootView() on an unattached View.");
        }

        final View rootView = view.getRootView();

        if (rootView == null) {
            return null;
        }

        final View rootContentView = rootView.findViewById(android.R.id.content);
        return rootContentView != null
                ? rootContentView
                : rootView;
    }
}
