package com.scheme.chc.lockscreen.lockscreen;

import android.annotation.SuppressLint;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.PixelFormat;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.CallLog;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.DigitalClock;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.ViewFlipper;

import com.scheme.chc.lockscreen.R;
import com.scheme.chc.lockscreen.service.LockScreenUtils;

import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;


@SuppressLint("NewApi")
public class SlideActivity extends AppCompatActivity implements View.OnTouchListener,
        LockScreenUtils.OnLockStatusChangedListener {

    @SuppressLint("StaticFieldLeak")
    public static Button btnUnlock;
    private ViewFlipper vfFlipper;
    private DigitalClock dcTime;
    private TextView tvDate, tvTemperature, tvWeather,
            tvPhoneStarting, tvPhoneNotification,
            tvMessageStarting, tvMessageNotification;
    private FloatingActionButton fabSlide;
    private ProgressBar pbBatteryStatus;

    private float dX, orX;
    private NotificationBlockView view;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;

    private LockScreenUtils mLockscreenUtils;
    private long userLeaveTime;
    public static boolean removeview = false;
    private FloatingActionButton fabCamera,fabPhone,fabMessage;
    private int NO_INTENT = 4;
    private int INTENT_CAMERA = 1;
    private int INTENT_PHONE = 0;
    private int INTENT_MESSAGES = 2;
    private boolean lockCameraAfterUse = false;
    private int TAKE_PICTURE_REQUEST = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide);
        initialize();
        OnClicks();
        configure();
    }

    @Override
    public void onAttachedToWindow() {
        // Set appropriate flags to make the screen appear over the keyguard
        this.getWindow().setType(WindowManager.LayoutParams.TYPE_KEYGUARD_DIALOG);
        this.getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_FULLSCREEN
                        | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        // Self added
                        | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_IGNORE_CHEEK_PRESSES
                        | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_SECURE
                        | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH
        );
        WindowManager.LayoutParams window = new WindowManager.LayoutParams();
        window.gravity = Gravity.TOP;
        super.onAttachedToWindow();
    }

    private void initialize() {
        vfFlipper = (ViewFlipper) findViewById(R.id.vfFlipper);
        dcTime = (DigitalClock) findViewById(R.id.dcTime);
        tvDate = (TextView) findViewById(R.id.tvDate);
        tvTemperature = (TextView) findViewById(R.id.tvTemperature);
        tvWeather = (TextView) findViewById(R.id.tvWeather);
        tvPhoneStarting = (TextView) findViewById(R.id.tvPhoneStarting);
        tvMessageStarting = (TextView) findViewById(R.id.tvMessageStarting);
        tvPhoneNotification = (TextView) findViewById(R.id.tvPhoneNotification);
        tvMessageNotification = (TextView) findViewById(R.id.tvMessageNotification);
        fabSlide = (FloatingActionButton) findViewById(R.id.fabSlide);
        fabCamera = (FloatingActionButton) findViewById(R.id.fabCamera);
        fabMessage = (FloatingActionButton) findViewById(R.id.fabMessage);
        fabPhone = (FloatingActionButton) findViewById(R.id.fabPhone);
        pbBatteryStatus = (ProgressBar) findViewById(R.id.pbBatteryStatus);
        btnUnlock = (Button) findViewById(R.id.btnUnlock);
        surfaceView = (SurfaceView) findViewById(R.id.mysurface);
        surfaceHolder = surfaceView.getHolder();

        mLockscreenUtils = new LockScreenUtils();

        myPhoneStateListener();
        lockHomeButton();
        blockNotificationBar(true);
        disableKeyguard();
    }

    private void OnClicks() {
        btnUnlock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                unlockHomeButton();
            }
        });
        fabPhone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flipNext(INTENT_PHONE);
            }
        });
        fabMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                flipNext(INTENT_MESSAGES);
            }
        });
        fabCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                lockCameraAfterUse = true;
                Intent cameraIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
//                File f = new File(android.os.Environment.getExternalStorageDirectory(), "image123.jpg");
//                Uri mImageCaptureUri = Uri.fromFile(f);
//                cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, mImageCaptureUri);
                startActivityForResult(cameraIntent, TAKE_PICTURE_REQUEST);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        System.out.println("activity");
        if (requestCode == TAKE_PICTURE_REQUEST) {
            if (resultCode == RESULT_OK) {
                startActivity(new Intent(this, SlideActivity.class));
            } else if (resultCode == RESULT_CANCELED) {
                startActivity(new Intent(this, SlideActivity.class));
            }
        }
    }

    @SuppressLint("SimpleDateFormat")
    private void configure() {
        Typeface tfDengXian = Typeface.createFromAsset(getAssets(), "DengXian.ttf");
        dcTime.setTypeface(tfDengXian);
        tvDate.setTypeface(tfDengXian);
        tvTemperature.setTypeface(tfDengXian);
        tvWeather.setTypeface(tfDengXian);
        tvPhoneStarting.setTypeface(tfDengXian);
        tvMessageStarting.setTypeface(tfDengXian);
        tvPhoneNotification.setTypeface(tfDengXian);
        tvMessageNotification.setTypeface(tfDengXian);
        fabSlide.setOnTouchListener(this);
        pbBatteryStatus.setProgress(((BatteryManager) getSystemService(BATTERY_SERVICE))
                .getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        );
        tvDate.setText(DateFormat.getDateTimeInstance().format(new Date()).substring(0,12) + ", " + (new SimpleDateFormat("EEEE").format(new Date())));
        tvPhoneNotification.setText(" " +getMissedCallsCount()+ " missed call(s)");
        tvMessageNotification.setText(" " +getMessagesCount()+ " new message(s)");
    }

    @NonNull
    @SuppressLint("Recycle")
    private String getMessagesCount() {
        Uri sms_content = Uri.parse("content://sms/inbox");
        Cursor c = this.getContentResolver().query(sms_content, null,"read = 0", null, null);
        assert c != null;
        return String.valueOf(c.getCount());
    }

    @NonNull
    @SuppressLint("Recycle")
    private String getMissedCallsCount() {
        String[] projection = { CallLog.Calls.CACHED_NAME, CallLog.Calls.CACHED_NUMBER_LABEL, CallLog.Calls.TYPE };
        String where = CallLog.Calls.TYPE + "=" + CallLog.Calls.MISSED_TYPE + " AND " + CallLog.Calls.NEW + "=1";
        Cursor c = this.getContentResolver().query(CallLog.Calls.CONTENT_URI, projection,where, null, null);
        assert c != null;
        return String.valueOf(c.getCount());
    }

    private void flipNext(int INTENT_CHOOSER) {
        vfFlipper.setInAnimation(this, R.anim.slide_in_left);
        vfFlipper.setOutAnimation(this, R.anim.slide_out_left);
        vfFlipper.showNext();

        startSurfaceViewThread(INTENT_CHOOSER);
    }

    private void blockNotificationBar(boolean add) {
        WindowManager manager = ((WindowManager) getApplicationContext().getSystemService(Context.WINDOW_SERVICE));
        if (add) {
            WindowManager.LayoutParams layoutParams = new WindowManager.LayoutParams();
            layoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
            layoutParams.gravity = Gravity.TOP;
            // FLAG_NOT_TOUCH_MODAL : This is to enable the notification to receive touch events
            // FLAG_LAYOUT_IN_SCREEN : Draws over status bar
            layoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                    | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL
                    | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;
            layoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
            layoutParams.height = (int) (50 * getResources().getDisplayMetrics().scaledDensity);
            layoutParams.format = PixelFormat.TRANSPARENT;
            view = new NotificationBlockView(this);
            manager.addView(view, layoutParams);
        } else  {
            manager.removeView(view);
            unlockHomeButton();
            enableKeyguard();
            SlideActivity.removeview = false;
        }
    }

    private void flipPrevious() {
        vfFlipper.setInAnimation(this, R.anim.slide_in_right);
        vfFlipper.setOutAnimation(this, R.anim.slide_out_right);
        vfFlipper.showPrevious();
    }

    private void startSurfaceViewThread(int intentchooser) {
        MainCanvasEngine mainCanvasEngine = new MainCanvasEngine(getApplicationContext(), true, surfaceHolder, surfaceView, intentchooser);
        mainCanvasEngine.start();
    }

    @Override
    public void onBackPressed() {
        // Override back button. App should not close.
        System.out.println("back");
        super.onBackPressed();
        if (vfFlipper.getDisplayedChild() > 0) {
            System.out.println("flipping");
            flipPrevious();
        }
    }

    // Lock home button
    public void lockHomeButton() {
        mLockscreenUtils.lock(SlideActivity.this);
    }

    // Unlock home button and wait for its callback
    public void unlockHomeButton() { mLockscreenUtils.unlock();}

    // Simply unlock device when home button is successfully unlocked
    @Override
    public void onLockStatusChanged(boolean isLocked) {
        if (!isLocked) {
            finish();
        }
    }

    @SuppressLint("InlinedApi")
    @RequiresApi(api = Build.VERSION_CODES.HONEYCOMB)
    @Override
    protected void onStop() {
        super.onStop();

        long defStop = System.currentTimeMillis() - userLeaveTime;
        if(lockCameraAfterUse) {
            lockCameraAfterUse = false;
            System.out.println("STOPing");
//            startActivity(new Intent(this, SlideActivity.class));
        }
        else if (defStop < 100) {       //else
            finish();
            startActivity(new Intent(this, SlideActivity.class));
            System.out.println("in stop");
        } else {
            // Back button or home
            if(removeview)
                blockNotificationBar(false);
            unlockHomeButton();
            System.out.println("STOP");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        System.out.println("resume");
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        System.out.println("restart");
    }

//    @Override
//    public void onSaveInstanceState(Bundle outState) {
//        // TODO Auto-generated method stub
//
//        outState.putString("photopath", photopath);
//
//
//        super.onSaveInstanceState(outState);
//    }
//
//    @Override
//    protected void onRestoreInstanceState(Bundle savedInstanceState) {
//        // TODO Auto-generated method stub
//        if (savedInstanceState != null) {
//            if (savedInstanceState.containsKey("photopath")) {
//                photopath = savedInstanceState.getString("photopath");
//            }
//        }
//
//        super.onRestoreInstanceState(savedInstanceState);
//    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        System.out.println("destroy");
    }

    @Override
    protected void onPause() {
        super.onPause();
        System.out.println("pause");
        if(removeview) {
            blockNotificationBar(false);
            // finish();
        }
        //else finish();
        if(lockCameraAfterUse) {
            lockCameraAfterUse = false;
            blockNotificationBar(false);
            // startActivity(new Intent(this, SlideActivity.class));
        }
    }

    @Override
    public void onUserLeaveHint() {
        super.onUserLeaveHint();
        userLeaveTime = System.currentTimeMillis();
    }

    // Handle button clicks
    @Override
    public boolean onKeyDown(int keyCode, android.view.KeyEvent event) {

        return (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN)
                || (keyCode == KeyEvent.KEYCODE_POWER)
                || (keyCode == KeyEvent.KEYCODE_VOLUME_UP)
                || (keyCode == KeyEvent.KEYCODE_CAMERA)
                || (keyCode == KeyEvent.KEYCODE_HOME)
                || (keyCode == KeyEvent.KEYCODE_APP_SWITCH);

    }

    // Handle the key press events here itself
    public boolean dispatchKeyEvent(KeyEvent event) {
        return !(event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_UP
                || (event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN)
                || (event.getKeyCode() == KeyEvent.KEYCODE_POWER))
                && (event.getKeyCode() == KeyEvent.KEYCODE_HOME)
                && (event.getKeyCode() == KeyEvent.KEYCODE_APP_SWITCH);
    }

    @Override
    public boolean onTouch(View view, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                orX = view.getX();
                dX = orX - event.getRawX();
                break;
            case MotionEvent.ACTION_MOVE:
                if (view.getX() <= 100.0) {
                    flipNext(NO_INTENT);
                }
                view.animate()
                        .x(event.getRawX() + dX)
                        .setDuration(0)
                        .start();
                System.out.println(event.getRawX() + dX);
                break;
            case MotionEvent.ACTION_UP:

                view.animate()
                        .x(orX)
                        .setDuration(500)
                        .start();
                break;
            default:
                return false;
        }
        return true;
    }

    @SuppressLint("NewApi")
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (!hasFocus) {
            System.out.println("in window");
            finish();
            startActivity(new Intent(this, SlideActivity.class));
        }
    }

    @SuppressWarnings("deprecation")
    private void disableKeyguard() {
        KeyguardManager mKM = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock mKL = mKM.newKeyguardLock("IN");
        mKL.disableKeyguard();
    }

    @SuppressWarnings("deprecation")
    private void enableKeyguard() {
        KeyguardManager mKM = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        KeyguardManager.KeyguardLock mKL = mKM.newKeyguardLock("IN");
        mKL.reenableKeyguard();
    }

    // Handle events of calls and unlock screen if necessary
    private class StateListener extends PhoneStateListener {
        @Override
        public void onCallStateChanged(int state, String incomingNumber) {

            super.onCallStateChanged(state, incomingNumber);
            switch (state) {
                case TelephonyManager.CALL_STATE_RINGING:
                    unlockHomeButton();
                    break;
                case TelephonyManager.CALL_STATE_OFFHOOK:
                    break;
                case TelephonyManager.CALL_STATE_IDLE:
                    break;
            }
        }
    }

    private void myPhoneStateListener() {
        // listen the events get fired during the call
        SlideActivity.StateListener phoneStateListener = new SlideActivity.StateListener();
        TelephonyManager telephonyManager = (TelephonyManager) this.getSystemService(TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

}
