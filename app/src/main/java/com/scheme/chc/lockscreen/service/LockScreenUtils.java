package com.scheme.chc.lockscreen.service;

import android.app.Activity;
import android.app.AlertDialog;
import android.support.annotation.NonNull;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;

import com.scheme.chc.lockscreen.R;

public class LockScreenUtils {

    // Member variables
    private OverlayDialog mOverlayDialog;
    private OnLockStatusChangedListener mLockStatusChangedListener;

    // Reset the variables
    public LockScreenUtils() {
        reset();
    }

    // Display overlay dialog with a view to prevent home button click
    public void lock(Activity activity) {
        if (mOverlayDialog == null) {
            mOverlayDialog = new OverlayDialog(activity);
            mOverlayDialog.show();
            mLockStatusChangedListener = (OnLockStatusChangedListener) activity;
        }
    }

    // Reset variables
    private void reset() {
        if (mOverlayDialog != null) {
            mOverlayDialog.dismiss();
            mOverlayDialog = null;
        }
    }

    // Unlock the home button and give callback to unlock the screen
    public void unlock() {
        if (mOverlayDialog != null) {
            mOverlayDialog.dismiss();
            mOverlayDialog = null;
            if (mLockStatusChangedListener != null) {
                mLockStatusChangedListener.onLockStatusChanged(false);
            }
        }
    }

    // Interface to communicate with owner activity
    public interface OnLockStatusChangedListener {
        void onLockStatusChanged(boolean isLocked);
    }

    // Create overlay dialog for locked screen to disable hardware buttons
    private static class OverlayDialog extends AlertDialog {

        OverlayDialog(Activity activity) {
            super(activity, R.style.OverlayDialog);
            WindowManager.LayoutParams params = getWindow().getAttributes();
            params.type = LayoutParams.TYPE_SYSTEM_ERROR;
            params.dimAmount = 0.0F;
            params.width = 0;
            params.height = 0;
            params.gravity = Gravity.BOTTOM;
            getWindow().setAttributes(params);
            getWindow().setFlags(LayoutParams.FLAG_SHOW_WHEN_LOCKED | LayoutParams.FLAG_NOT_TOUCH_MODAL, 0xffffff);
            setOwnerActivity(activity);
            setCancelable(false);
        }

        // consume touch events
        public final boolean dispatchTouchEvent(@NonNull MotionEvent motionevent) {
            return true;
        }
    }
}
