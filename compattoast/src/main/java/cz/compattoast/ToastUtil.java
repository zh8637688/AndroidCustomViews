package cz.compattoast;

import android.app.Activity;
import android.content.Context;
import android.graphics.PixelFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;

/**
 * 避免用户关闭通知权限、悬浮窗权限导致Toast无法展示
 * Created by haozhou on 2017/8/9.
 * @author haozhou
 */
public class ToastUtil {
    private static final int LENGTH_SHORT = 1500;
    private static final int LENGTH_LONG = 3000;

    private static Toast systemToast;
    private static DialogToast dialogToast;
    private static ViewToast viewToast;

    public static void showToast(Context context, String string) {
        showToast(context, string, -1);
    }

    public static void showToast(Context context, String string, int iconId) {
        showToast(context, string, iconId, Toast.LENGTH_SHORT);
    }

    public static void showToast(Context context, String string, int iconId, int duration) {
        if (AlertWindowUtil.isAlertWindowEnable(context)) {
            // 悬浮窗实现
            showDialogToast(context, string, iconId, duration);
        } else if (context instanceof Activity) {
            // View实现
            showViewToast((Activity) context, string, iconId, duration);
        } else {
            // 系统toast
            showSystemToast(context, string, iconId, duration);
        }
    }

    public static void showSystemToast(Context context, String string, int iconId, int duration) {
        if (systemToast == null) {
            systemToast = createSystemToast(context);
        }
        ((TextView) systemToast.getView().findViewById(R.id.text)).setText(string);
        ImageView icon = (ImageView) systemToast.getView().findViewById(R.id.icon);
        if (iconId > 0) {
            icon.setImageResource(iconId);
            icon.setVisibility(View.VISIBLE);
        } else {
            icon.setVisibility(View.GONE);
        }
        systemToast.setDuration(duration);
        systemToast.show();
    }

    private static Toast createSystemToast(Context context) {
        context = context.getApplicationContext();
        Toast toast = new Toast(context);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setView(LayoutInflater.from(context).inflate(R.layout.layout_toast, null));
        return toast;
    }

    public static void showDialogToast(Context context, String string, int iconId, int duration) {
        if (dialogToast == null) {
            dialogToast = DialogToast.makeToast(context, string, iconId, duration);
        } else {
            dialogToast.setText(string);
            dialogToast.setIcon(iconId);
            dialogToast.setDuration(duration);
        }

        dialogToast.show();
    }

    public static void showViewToast(Activity activity, String string, int iconId, int duration) {
        try {
            if (viewToast == null || viewToast.getActivity() != activity) {
                viewToast = new ViewToast(activity);
            }
            viewToast.setText(string);
            viewToast.setIcon(iconId);
            viewToast.setDuration(duration);
            viewToast.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static class DialogToast {

        private View toastView = null;
        private TextView text;
        private ImageView icon;

        private WindowManager wm;
        private int duration;

        private Runnable cancelTask = new Runnable() {
            @Override
            public void run() {
                if (toastView.getParent() != null) {
                    wm.removeView(toastView);
                }
            }
        };

        private DialogToast(Context context) {
            toastView = createToastView(context);
            text = (TextView) toastView.findViewById(R.id.text);
            icon = (ImageView) toastView.findViewById(R.id.icon);

            wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        }

        static DialogToast makeToast(Context context, String string, int resID, int duration) {
            DialogToast toast = new DialogToast(context);
            toast.setText(string);
            toast.setIcon(resID);
            toast.setDuration(duration);
            return toast;
        }

        void show() {
            if (toastView.getParent() == null) {
                wm.addView(toastView, createWindowParams());
            } else {
                toastView.removeCallbacks(cancelTask);
            }
            toastView.postDelayed(cancelTask, duration);
        }

        public void setText(String string) {
            text.setText(string);
        }

        public void setIcon(int resID) {
            if (resID > 0) {
                icon.setImageResource(resID);
                icon.setVisibility(View.VISIBLE);
            } else {
                icon.setVisibility(View.GONE);
            }
        }

        public void setDuration(int dur) {
            duration = dur == Toast.LENGTH_SHORT ? LENGTH_SHORT : LENGTH_LONG;
        }

        private View createToastView(Context context) {
            LayoutInflater inflater = LayoutInflater.from(context);
            return inflater.inflate(R.layout.layout_toast, null);
        }

        private WindowManager.LayoutParams createWindowParams() {
            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.CENTER;
            params.format = PixelFormat.TRANSLUCENT;
            params.type = WindowManager.LayoutParams.TYPE_SYSTEM_ALERT;
            params.windowAnimations = R.style.toast;
            params.flags = WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE;
            return params;
        }
    }

    private static class ViewToast {
        private WeakReference<Activity> activityRef;

        private FrameLayout rootView;
        private View toastView;
        private long duration;

        private Animation enterAnim;
        private Animation exitAnim;

        private Animation.AnimationListener exitAnimListener;

        private Runnable cancelTask;

        ViewToast(Activity activity) {
            activityRef = new WeakReference<Activity>(activity);

            rootView = (FrameLayout) activity.findViewById(android.R.id.content);

            toastView = activity.getLayoutInflater().inflate(R.layout.layout_toast, rootView, false);
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) toastView.getLayoutParams();
            params.gravity = Gravity.CENTER;

            enterAnim = AnimationUtils.loadAnimation(activity, R.anim.toast_enter);
            exitAnim = AnimationUtils.loadAnimation(activity, R.anim.toast_exit);
            exitAnimListener = new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    rootView.removeView(toastView);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            };

            cancelTask = new Runnable() {
                @Override
                public void run() {
                    exitAnim.setAnimationListener(exitAnimListener);
                    toastView.startAnimation(exitAnim);
                }
            };
        }

        void setText(String text) {
            ((TextView) toastView.findViewById(R.id.text)).setText(text);
        }

        void setIcon(int resID) {
            ImageView imageView = (ImageView) toastView.findViewById(R.id.icon);
            if (resID > 0) {
                imageView.setImageResource(resID);
                imageView.setVisibility(View.VISIBLE);
            } else {
                imageView.setVisibility(View.GONE);
            }
        }

        void setDuration(int dur) {
            duration = dur == Toast.LENGTH_SHORT ? LENGTH_SHORT : LENGTH_LONG;
        }

        void show() {
            exitAnim.setAnimationListener(null);
            toastView.removeCallbacks(cancelTask);
            toastView.clearAnimation();

            if (toastView.getParent() == null) {
                rootView.addView(toastView);
                toastView.startAnimation(enterAnim);
            }

            toastView.postDelayed(cancelTask, duration);
        }

        Activity getActivity() {
            return activityRef.get();
        }
    }
}
