package cz.slidetoastlayout;

import android.graphics.Color;
import android.os.Bundle;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.AppCompatActivity;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TextView toastHeader;
    private SlideToastLayout toastLayout;
    private SwipeRefreshLayout refreshLayout;
    private Adapter adapter;

    private List<Integer> dataList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        toastHeader = genToastHeader();
        toastLayout = (SlideToastLayout) findViewById(R.id.toast_layout);
        toastLayout.setToastView(toastHeader);

        refreshLayout = (SwipeRefreshLayout)findViewById(R.id.refreshLayout);
        refreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshLayout.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        int newData = produceData();
                        toastHeader.setText("新增数据\"" + newData + "\"条");
                        toastLayout.showToast();
                        adapter.notifyDataSetChanged();
                        refreshLayout.setRefreshing(false);
                    }
                }, 1000);
            }
        });

        dataList = new ArrayList<>();
        produceData();

        adapter = new Adapter();
        ListView listView = (ListView) findViewById(R.id.listView);
        listView.setAdapter(adapter);
    }

    private TextView genToastHeader() {
        TextView toastHeader = new TextView(this);
        toastHeader.setTextSize(14);
        toastHeader.setTextColor(0xffb69273);
        toastHeader.setBackgroundColor(0xfffff7e3);
        toastHeader.setGravity(Gravity.CENTER);
        toastHeader.setPadding(0, 10, 0, 10);
        return toastHeader;
    }

    private int produceData() {
        int dataSize = new Random().nextInt(30);
        for (int i = 0; i < dataSize; i++) {
            dataList.add(dataList.size());
        }
        return dataSize;
    }

    private class Adapter extends BaseAdapter {

        @Override
        public int getCount() {
            return dataList.size();
        }

        @Override
        public Integer getItem(int position) {
            return dataList.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new TextView(getApplicationContext());
                convertView.setPadding(0, 10, 0, 10);
                ((TextView)convertView).setGravity(Gravity.CENTER);
                ((TextView)convertView).setTextSize(24);
                ((TextView)convertView).setTextColor(Color.BLACK);
            }
            ((TextView)convertView).setText(String.valueOf(getItem(position)));
            return convertView;
        }
    }
}
