package cz.observelistview;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    TextView tv;
    ObserveListView list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        list = findViewById(R.id.list);
        list.setAdapter(new BaseAdapter() {
            @Override
            public int getCount() {
                return 10;
            }

            @Override
            public Object getItem(int i) {
                return null;
            }

            @Override
            public long getItemId(int i) {
                return 0;
            }

            @Override
            public int getViewTypeCount() {
                return 2;
            }

            @Override
            public int getItemViewType(int pos) {
                return pos % 2;
            }

            @Override
            public View getView(int i, View view, ViewGroup viewGroup) {
                if (view == null) {
                    view = new TextView(MainActivity.this);
                    ((TextView) view).setTextColor(Color.BLACK);
                    if (getItemViewType(i) == 0) {
                        ((TextView) view).setTextSize(40);
                    } else {
                        ((TextView) view).setTextSize(26);
                    }
                }
                ((TextView) view).setText("" + i);
                return view;
            }
        });
        list.setOnObserverScrollListener(new ObserveListView.OnObserverScrollListener() {
            public void onScroll(ObserveListView.ScrollState state, int scrollY) {
                tv.setText("Scroll: " + scrollY);
            }
        });
        list.setTouchInterceptionViewGroup((ScrollView) findViewById(R.id.scroll));
        tv = findViewById(R.id.hint);
    }
}
