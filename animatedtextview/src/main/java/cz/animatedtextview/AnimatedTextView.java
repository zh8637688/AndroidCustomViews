package cz.animatedtextview;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.AnimationDrawable;
import android.graphics.drawable.Drawable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.style.DynamicDrawableSpan;
import android.util.AttributeSet;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * 支持AnimationDrawable Span
 * Created by haozhou on 2017/9/11.
 */

public class AnimatedTextView extends TextView {
    private ArrayList<AnimationDrawable> animationDrawables;

    public AnimatedTextView(Context context) {
        super(context);
    }

    public AnimatedTextView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public AnimatedTextView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public void invalidateDrawable(@NonNull Drawable drawable) {
        if (verifyDrawableInternal(drawable)) {
            invalidate();
        } else {
            super.invalidateDrawable(drawable);
        }
    }

    @Override
    protected boolean verifyDrawable(@NonNull Drawable who) {
        return super.verifyDrawable(who) || verifyDrawableInternal(who);
    }

    private boolean verifyDrawableInternal(Drawable who) {
        if (animationDrawables != null) {
            for (Drawable dr : animationDrawables) {
                if (who == dr) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public void setText(CharSequence text, BufferType type) {
        super.setText(text, type);

        if (text instanceof Spannable) {
            Spannable spannable = (Spannable) text;
            DynamicDrawableSpan[] spans = spannable.getSpans(0, text.length(), DynamicDrawableSpan.class);
            animationDrawables = new ArrayList<>();
            for (DynamicDrawableSpan span : spans) {
                Drawable drawable = span.getDrawable();
                if (drawable instanceof AnimationDrawable) {
                    drawable.setCallback(this);
                    animationDrawables.add((AnimationDrawable) span.getDrawable());
                }
            }
        }
    }

    /**
     * 支持Span设置padding
     */
    public static class PaddingDrawableSpan extends DynamicDrawableSpan {
        private Drawable drawable;
        private int paddingLeft, paddingTop, paddingRight, paddingBottom;

        public PaddingDrawableSpan(Drawable drawable) {
            this.drawable = drawable;
        }

        @Override
        public Drawable getDrawable() {
            return drawable;
        }

        public void setPadding(int paddingLeft, int paddingTop, int paddingRight, int paddingBottom) {
            this.paddingLeft = paddingLeft;
            this.paddingTop = paddingTop;
            this.paddingRight = paddingRight;
            this.paddingBottom = paddingBottom;
        }

        @Override
        public int getSize(Paint paint, CharSequence text, int start, int end, Paint.FontMetricsInt fm) {
            Drawable d = getDrawable();
            Rect rect = d.getBounds();

            if (fm != null) {
                fm.ascent = -rect.bottom - paddingTop;
                fm.descent = paddingBottom;

                fm.top = fm.ascent;
                fm.bottom = paddingBottom;
            }

            return rect.right + paddingLeft + paddingRight;
        }

        @Override
        public void draw(Canvas canvas, CharSequence text,
                         int start, int end, float x,
                         int top, int y, int bottom, Paint paint) {
            Drawable b = getDrawable();
            canvas.save();
            canvas.translate(x + paddingLeft,
                    (bottom + top - b.getBounds().bottom) / 2);
            b.draw(canvas);
            canvas.restore();
        }
    }
}
