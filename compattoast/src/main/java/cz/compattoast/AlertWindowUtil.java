package cz.compattoast;

import android.annotation.TargetApi;
import android.app.AppOpsManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.graphics.PixelFormat;
import android.os.Build;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * 悬浮窗权限相关工具
 * @author haozhou
 */
public class AlertWindowUtil {
    // 是否可以使用TYPE_TOAST类型的悬浮窗
    private static final String SP_KEY_CAN_USE_TYPE_TOAST = "key_can_use_type_toast";
    // 是否已经模拟测试过TYPE_TOAST
    private static final String SP_KEY_TYPE_TOAST_SIMULATED = "key_type_toast_simulated";

    private static SharedPreferences getSP(Context context) {
        return context.getSharedPreferences("Toast", Context.MODE_PRIVATE);
    }

    public static boolean isAlertWindowEnable(Context context) {
        if (Build.VERSION.SDK_INT >= 23) {
            return Settings.canDrawOverlays(context);
        } else if (Build.VERSION.SDK_INT >= 19) {
            return isAlertWindowEnableKitKat(context);
        } else {
            return true;
        }
    }

    public static boolean isAlertWindowEnableWithTypeToast(Context context) {
        SharedPreferences sp = getSP(context);
        boolean simulated = sp.getBoolean(SP_KEY_TYPE_TOAST_SIMULATED, false);
        if (!simulated) {
            simulateAddWindowTypeToast(context);
            return false;
        } else {
            return sp.getBoolean(SP_KEY_CAN_USE_TYPE_TOAST, false);
        }
    }

    private static boolean isAlertWindowEnableKitKat(Context context) {
        return checkOpNoThrow(context, "OP_SYSTEM_ALERT_WINDOW");
    }

    @TargetApi(19)
    private static boolean checkOpNoThrow(Context context, String op) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        ApplicationInfo appInfo = context.getApplicationInfo();
        String pkg = context.getApplicationContext().getPackageName();
        int uid = appInfo.uid;
        try {
            Class<?> appOpsClass = Class.forName(AppOpsManager.class.getName());
            Method checkOpNoThrowMethod = appOpsClass.getMethod("checkOpNoThrow", Integer.TYPE,
                    Integer.TYPE, String.class);
            Field opPostNotificationValue = appOpsClass.getDeclaredField(op);
            int value = (int) opPostNotificationValue.get(Integer.class);
            return ((int) checkOpNoThrowMethod.invoke(appOps, value, uid, pkg)
                    == AppOpsManager.MODE_ALLOWED);
        } catch (ClassNotFoundException | NoSuchMethodException | NoSuchFieldException |
                InvocationTargetException | IllegalAccessException | RuntimeException e) {
            return true;
        }
    }

    // 大部分手机在关闭悬浮窗权限后还能通过 TYPE_TOAST 创建悬浮窗
    private static void simulateAddWindowTypeToast(final Context context) {
        if (Build.VERSION.SDK_INT > 18 &&
                Build.VERSION.SDK_INT <= 25) {
            final WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
            final View invisibleView = new View(context);
            invisibleView.addOnAttachStateChangeListener(new View.OnAttachStateChangeListener() {
                @Override
                public void onViewAttachedToWindow(View v) {
                    // 结束测试，删除屏幕上的View
                    wm.removeView(invisibleView);
                    WindowManager.LayoutParams param = (WindowManager.LayoutParams) v.getLayoutParams();
                    if (param.type != WindowManager.LayoutParams.TYPE_TOAST) {
                        // 在部分厂商定制系统上，可能将type修改，导致悬浮窗无法展示
                        // 比如MiUI 8上，执行WindowManager.addView会检查权限并将type修改成TYPE_SYSTEM_ALERT
                        // ref("android.view.ViewRootImplInjector.transformWindowType")
                        getSP(context).edit().putBoolean(SP_KEY_CAN_USE_TYPE_TOAST, false).apply();
                    } else {
                        // 暂时认定这类情况能够展示悬浮窗
                        getSP(context).edit().putBoolean(SP_KEY_CAN_USE_TYPE_TOAST, true).apply();
                    }
                }

                @Override
                public void onViewDetachedFromWindow(View v) {

                }
            });

            WindowManager.LayoutParams params = new WindowManager.LayoutParams();
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            params.width = WindowManager.LayoutParams.WRAP_CONTENT;
            params.gravity = Gravity.TOP | Gravity.LEFT;
            params.format = PixelFormat.TRANSLUCENT;
            // 测试 TYPE_TOAST
            params.type = WindowManager.LayoutParams.TYPE_TOAST;
            addViewToWindow(wm, invisibleView, params);
        }

        getSP(context).edit().putBoolean(SP_KEY_TYPE_TOAST_SIMULATED, true).apply();
    }

    public static void addViewToWindow(WindowManager wm, View view, WindowManager.LayoutParams params) {
        beforeAddToWindow(params);
        wm.addView(view, params);
        afterAddToWindow();
    }

    private static void beforeAddToWindow(WindowManager.LayoutParams params) {
        setMiUI_International(true);
        setMeizuParams(params);
    }

    private static void afterAddToWindow() {
        setMiUI_International(false);
    }

    /*
     * MiUI国际版并未对悬浮窗进行限制
     */
    private static void setMiUI_International(boolean flag) {
        try {
            Class BuildForMi = Class.forName("miui.os.Build");
            Field isInternational = BuildForMi.getDeclaredField("IS_INTERNATIONAL_BUILD");
            isInternational.setAccessible(true);
            isInternational.setBoolean(null, flag);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /*
     * 适配魅族
     */
    private static void setMeizuParams(WindowManager.LayoutParams params) {
        try {
            Class MeizuParamsClass = Class.forName("android.view.MeizuLayoutParams");
            Field flagField = MeizuParamsClass.getDeclaredField("flags");
            flagField.setAccessible(true);
            Object MeizuParams = MeizuParamsClass.newInstance();
            flagField.setInt(MeizuParams, 0x40);

            Field mzParamsField = params.getClass().getField("meizuParams");
            mzParamsField.set(params, MeizuParams);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
