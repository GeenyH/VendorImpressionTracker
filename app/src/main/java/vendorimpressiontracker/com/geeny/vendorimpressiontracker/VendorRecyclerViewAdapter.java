package vendorimpressiontracker.com.geeny.vendorimpressiontracker;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.List;

import vendorimpressiontracker.com.geeny.vendorimpressiontracker.Impression.ImpressionTracker;

class VendorRecyclerViewAdapter extends RecyclerView.Adapter {

    private Context mContext;
    private List<String> mVendors;
    private ImpressionTracker mImpressionTracker;

    public VendorRecyclerViewAdapter(Context context) {
        this.mContext = context;
        mImpressionTracker = new ImpressionTracker(context);
    }

    public List<String> getData() {
        return mVendors;
    }

    public void setData(List<String> vendorInfos) {
        mVendors = vendorInfos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        View view = inflater.inflate(R.layout.item_vendor_info, parent, false);
        return new VendorViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (mVendors != null) {
            String vendorInfo = mVendors.get(position);
            TextView tvVendorInfo = holder.itemView.findViewById(R.id.tv_vendor_info);
            if (position % 2 == 0) {
                holder.itemView.setBackgroundColor(mContext.getColor(R.color.vendorBackgroundColor2));
            } else {
                holder.itemView.setBackgroundColor(mContext.getColor(R.color.vendorBackgroundColor1));
            }
            tvVendorInfo.setText(vendorInfo);
        }
    }

    @Override
    public int getItemCount() {
        return mVendors == null ? 0 : mVendors.size();
    }

    public void trackImpression(View view, int position) {
        String vendorInfo = getData().get(position);
        mImpressionTracker.addView(vendorInfo, view, mImpressionTracker.getVendorCardImpression(() -> {
            Log.d("Vendor Impression", String.format("The user had focused %s for more than a second.", vendorInfo.toUpperCase()));
        }));
    }

    public void destoryImpressionTracker() {
        if (mImpressionTracker != null) {
            mImpressionTracker.destroy();
        }
    }

    class VendorViewHolder extends RecyclerView.ViewHolder {
        public VendorViewHolder(View itemView) {
            super(itemView);
        }
    }
}
