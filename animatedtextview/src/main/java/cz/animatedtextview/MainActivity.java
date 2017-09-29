package cz.animatedtextview;

import android.app.Activity;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AnimationDrawable drawable = (AnimationDrawable) ContextCompat
                .getDrawable(this, R.drawable.loading);
        drawable.setBounds(0, 0, 160, 160);

        AnimatedTextView.PaddingDrawableSpan span = new AnimatedTextView
                .PaddingDrawableSpan(drawable);
        span.setPadding(30, 40, 50, 60);

        String text = getString(R.string.text);
        int spanStartIndex = text.indexOf("pic");
        SpannableString spannable = new SpannableString(text);
        spannable.setSpan(span, spanStartIndex, spanStartIndex + "pic".length(),
                Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        TextView textView = (TextView) findViewById(R.id.animatedTextView);
        textView.setText(spannable);

        drawable.start();
    }
}
