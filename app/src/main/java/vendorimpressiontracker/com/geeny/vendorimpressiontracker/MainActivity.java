package vendorimpressiontracker.com.geeny.vendorimpressiontracker;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

import java.util.LinkedList;
import java.util.List;

import vendorimpressiontracker.com.geeny.vendorimpressiontracker.Impression.DeviceRelatedUtil;

public class MainActivity extends AppCompatActivity {

    private LinearLayoutManager mLinearLayoutManager;
    private VendorRecyclerViewAdapter mVendorRecyclerViewAdapter;
    private RecyclerView mRvVendors;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        DeviceRelatedUtil.init(this);

        initView();
        initData();
    }

    private void initView() {
        mRvVendors = findViewById(R.id.rv_vendor_list);
    }

    private void initData() {
        List<String> vendorInfos = new LinkedList<>();
        for (int i = 1; i < 100; i++) {
            vendorInfos.add(String.format("Vendor %s", String.valueOf(i)));
        }

        mVendorRecyclerViewAdapter = new VendorRecyclerViewAdapter(MainActivity.this);
        mLinearLayoutManager = new LinearLayoutManager(MainActivity.this);

        mRvVendors.setLayoutManager(mLinearLayoutManager);
        mRvVendors.setAdapter(mVendorRecyclerViewAdapter);

        mVendorRecyclerViewAdapter.setData(vendorInfos);

        mRvVendors.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                trackVendorCarouselImpression();
            }
        });
    }

    private void trackVendorCarouselImpression() {
        if (mVendorRecyclerViewAdapter != null && mVendorRecyclerViewAdapter.getData() != null) {
            addVendorViewForImpressionTracker();
        }
    }

    private void addVendorViewForImpressionTracker() {
        int startPosition = mLinearLayoutManager.findFirstVisibleItemPosition();
        if (startPosition == RecyclerView.NO_POSITION) {
            return;
        }
        int endPosition = mLinearLayoutManager.findLastVisibleItemPosition();

        for (int i = startPosition; i <= endPosition; i++) {
            View view = mLinearLayoutManager.findViewByPosition(i);
            mVendorRecyclerViewAdapter.trackImpression(view, i);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVendorRecyclerViewAdapter != null) {
            mVendorRecyclerViewAdapter.destoryImpressionTracker();
        }
    }
}
