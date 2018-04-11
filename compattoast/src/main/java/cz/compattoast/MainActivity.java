package cz.compattoast;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        findViewById(R.id.system).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToastUtil.showSystemToast(MainActivity.this, "系统级别Toast", R.drawable.toast, Toast.LENGTH_SHORT);
            }
        });
        findViewById(R.id.view).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToastUtil.showViewToast(MainActivity.this, "View级别Toast", R.drawable.toast, Toast.LENGTH_SHORT);
            }
        });
        findViewById(R.id.dialog).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ToastUtil.showDialogToast(MainActivity.this, "Dialog级别Toast", R.drawable.toast, Toast.LENGTH_SHORT);
            }
        });
    }
}
