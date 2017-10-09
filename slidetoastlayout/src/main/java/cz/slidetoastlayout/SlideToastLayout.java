package cz.slidetoastlayout;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.support.v4.view.ViewCompat;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.LinearLayout;

import java.lang.ref.WeakReference;

/**
 * 下拉式Toast
 * Created by haozhou on 2017/8/25.
 */
public class SlideToastLayout extends LinearLayout {
    private View toastHeader;

    private Interpolator mScrollAnimationInterpolator;
    private SmoothScrollRunnable mCurrentSmoothScrollRunnable;
    private Handler toastHandler;

    private int toastHeight;

    public SlideToastLayout(Context context) {
        this(context, null);
    }

    public SlideToastLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
        toastHandler = new ToastHandler(this);
    }

    public void setToastView(View toastView) {
        if (toastHeader != null) {
            removeView(toastHeader);
        }

        toastHeader = toastView;
        if (toastHeader != null) {
            ViewGroup.LayoutParams layoutParams = toastHeader.getLayoutParams();
            if (layoutParams == null) {
                layoutParams = new LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
            }
            addView(toastHeader, 0, layoutParams);
            getViewTreeObserver().addOnPreDrawListener(
                    new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            getViewTreeObserver().removeOnPreDrawListener(this);
                            toastHeight = toastHeader.getMeasuredHeight();
                            scrollTo(0, toastHeight);
                            setPadding(getPaddingLeft(), getPaddingTop(),
                                    getPaddingRight(), getPaddingBottom() - toastHeight);
                            return false;
                        }
                    });
        }
    }

    public void showToast() {
        showToast(false);
    }

    public void showToast(boolean persist) {
        scrollTo(0, 0);

        if (toastHandler.hasMessages(ToastHandler.MSG_HIDE_TOAST)) {
            toastHandler.removeMessages(ToastHandler.MSG_HIDE_TOAST);
        }

        if (!persist) {
            toastHandler.sendEmptyMessageDelayed(ToastHandler.MSG_HIDE_TOAST, 1500);
        }
    }

    public void hideToast() {
        smoothScrollTo(toastHeight, 300, null);
    }

    private void smoothScrollTo(int newScrollValue, long duration, OnSmoothScrollFinishedListener listener) {
        if (null != mCurrentSmoothScrollRunnable) {
            mCurrentSmoothScrollRunnable.stop();
        }

        final int oldScrollValue = getScrollY();

        if (oldScrollValue != newScrollValue) {
            if (null == mScrollAnimationInterpolator) {
                // Default interpolator is a Decelerate Interpolator
                mScrollAnimationInterpolator = new DecelerateInterpolator();
            }
            mCurrentSmoothScrollRunnable = new SmoothScrollRunnable(oldScrollValue, newScrollValue, duration, listener);

            post(mCurrentSmoothScrollRunnable);
        }
    }

    private static class ToastHandler extends Handler {
        static final int MSG_HIDE_TOAST = 1;

        WeakReference<SlideToastLayout> ref;

        ToastHandler(SlideToastLayout listView) {
            ref = new WeakReference<>(listView);
        }

        @Override
        public void handleMessage(Message msg) {
            SlideToastLayout listView = ref.get();
            if (listView != null) {
                switch (msg.what) {
                    case MSG_HIDE_TOAST:
                        listView.hideToast();
                        break;
                }
            }
        }
    }

    private class SmoothScrollRunnable implements Runnable {
        private final Interpolator mInterpolator;
        private final int mScrollToY;
        private final int mScrollFromY;
        private final long mDuration;
        private OnSmoothScrollFinishedListener mListener;

        private boolean mContinueRunning = true;
        private long mStartTime = -1;
        private int mCurrentY = -1;

        SmoothScrollRunnable(int fromY, int toY, long duration, OnSmoothScrollFinishedListener listener) {
            mScrollFromY = fromY;
            mScrollToY = toY;
            mInterpolator = mScrollAnimationInterpolator;
            mDuration = duration;
            mListener = listener;
        }

        @Override
        public void run() {

            /*
             * Only set mStartTime if this is the first time we're starting,
             * else actually calculate the Y delta
             */
            if (mStartTime == -1) {
                mStartTime = System.currentTimeMillis();
            } else {

                /*
                 * We do do all calculations in long to reduce software float
                 * calculations. We use 1000 as it gives us good accuracy and
                 * small rounding errors
                 */
                long normalizedTime = (1000 * (System.currentTimeMillis() - mStartTime)) / mDuration;
                normalizedTime = Math.max(Math.min(normalizedTime, 1000), 0);

                final int deltaY = Math.round((mScrollFromY - mScrollToY)
                        * mInterpolator.getInterpolation(normalizedTime / 1000f));
                mCurrentY = mScrollFromY - deltaY;
                scrollTo(0, mCurrentY);
            }

            // If we're not at the target Y, keep going...
            if (mContinueRunning && mScrollToY != mCurrentY) {
                ViewCompat.postOnAnimation(SlideToastLayout.this, this);
            } else {
                if (null != mListener) {
                    mListener.onSmoothScrollFinished();
                }
            }
        }

        void stop() {
            mContinueRunning = false;
            removeCallbacks(this);
        }
    }

    interface OnSmoothScrollFinishedListener {
        void onSmoothScrollFinished();
    }
}
