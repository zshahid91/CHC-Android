package com.scheme.chc.lockscreen.lockscreen;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Vibrator;
import android.provider.CallLog;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Toast;

import com.scheme.chc.lockscreen.service.LockScreenUtils;
import com.scheme.chc.lockscreen.utils.GrahamScan;
import com.scheme.chc.lockscreen.utils.Icon;
import com.scheme.chc.lockscreen.utils.IconPool;
import com.scheme.chc.lockscreen.utils.Utilities;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

class MainCanvasEngine extends Thread implements View.OnTouchListener {

    private final int intentchooser;
    private Utilities utilities;
    private IconPool iconPool;
    private Context context;
    private Vibrator vibrator;
    private SurfaceView surfaceView;
    private SurfaceHolder surfaceHolder;

    private int totalIconsInAssets;
    private int canvasWidth, canvasHeight;
    private int draw, boundary, wrongTries;

    private ArrayList<Integer> passIconsXList;
    private ArrayList<Integer> passIconsYList;
    private ArrayList<Bitmap> bitmapArrayList;
    private ArrayList<Bitmap> selectedPassIcons;
    private ArrayList<Icon> iconsArrayFromAssets;

    private Path path;
    private Paint paint;
    private boolean lock;

    MainCanvasEngine(Context context, boolean lock, SurfaceHolder surfaceHolder, SurfaceView surfaceView, int intentchooser) {
        this.utilities = Utilities.getInstance();
        this.iconPool = IconPool.getInstance();
        this.context = context;
        this.surfaceView = surfaceView;
        this.surfaceHolder = surfaceHolder;
        this.intentchooser = intentchooser;
        this.vibrator = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);

        this.draw = 1;
        this.boundary = 0;
        this.wrongTries = 1;
        this.totalIconsInAssets = 0;

        this.passIconsXList = new ArrayList<>();
        this.passIconsYList = new ArrayList<>();
        this.bitmapArrayList = new ArrayList<>();
        this.selectedPassIcons = new ArrayList<>();
        this.iconsArrayFromAssets = new ArrayList<>();

        this.lock = lock;
        this.path = new Path();
        this.paint = new Paint();

    }

    @Override
    public void run() {
        super.run();
        while (lock) {
            // Checks if the lockCanvas() method will be success, and if not,
            // will check this statement again
            if (!surfaceHolder.getSurface().isValid()) {
                continue;
            }
            draw();
            lock = false;
        }
    }

    private void draw() {
        /* Start editing pixels in this surface.*/
        Canvas canvas = surfaceHolder.lockCanvas();
        canvas.drawColor(Color.DKGRAY, PorterDuff.Mode.CLEAR);
        surfaceView.setOnTouchListener(this);

        initializeUtilities(canvas);
        getAllIconsFromAssets();
        drawIcons(paint, canvas);
        createConvexHull();
        drawPolygon(paint, canvas);

        // End of painting to canvas. system will paint with this canvas,to the surface.
        surfaceHolder.unlockCanvasAndPost(canvas);
    }

    private void initializeUtilities(Canvas canvas) {
        canvasWidth = canvas.getWidth();
        canvasHeight = canvas.getHeight();
        selectedPassIcons.clear();

        String[] passIcons = utilities.getChosenPassIcons();
        if (passIcons.length > 1) {
            for (int i = 0; i < utilities.getTotalNumberOfPassIcons(); i++) {
                selectedPassIcons.add(getFilenameFromAssets(passIcons[i]));
            }
        } else {
            passIcons = utilities.getUploadedPassIcons();
        }
        if (passIcons.length > 1) {
            for (int i = 0; i < utilities.getTotalNumberOfPassIcons(); i++) {
                selectedPassIcons.add(convertUriToBitmap(Uri.parse(passIcons[i])));
            }
        }
    }

    private void getAllIconsFromAssets() {
        try {
            iconsArrayFromAssets.clear();
            iconsArrayFromAssets.addAll(iconPool.getIconPool());
            if (utilities.getChosenPassIcons().length <= 1
                    && utilities.getUploadedPassIcons().length <= 1) {
                for (int i = 1; i <= utilities.getTotalNumberOfPassIcons(); i++) {
                    selectedPassIcons.add(bitmapFromAssets(context.getAssets().open("icons/" + i + ".png")));
                }
            }
            totalIconsInAssets = iconsArrayFromAssets.size() - utilities.getTotalNumberOfPassIcons();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @SuppressLint("NewApi")
    private void drawIcons(Paint paint, Canvas canvas) {
        bitmapArrayList.clear();
        int iconTopSpace = 5;
        int iconLeftSpace = 5;
        passIconsXList.clear();
        passIconsYList.clear();
        utilities.index.clear();
        path.reset();

        for (int i = 0; i < utilities.getTotalNumberOfPassIcons(); i++) {
            bitmapArrayList.add(selectedPassIcons.get(i));
        }

        for (int i = 0; i < utilities.getTotalNumberOfRegularIcons(); i++) {
            int randomNumber = utilities.getRandomInt(totalIconsInAssets);
            Bitmap drawingBitmap = (iconsArrayFromAssets.get(randomNumber)).getImageData();
            boolean same = false;
            for (Bitmap bitmap : bitmapArrayList) {
                same = bitmap.sameAs(drawingBitmap);
            }
            if (!same) {
                bitmapArrayList.add(drawingBitmap);
            } else
                System.out.println("Not adding");
        }

        Collections.shuffle(bitmapArrayList, new Random(System.nanoTime()));

        for (int i = 0; i < utilities.getTotalIconsToDisplay(); i++) {
            canvas.drawBitmap(bitmapArrayList.get(i), iconLeftSpace, iconTopSpace, paint);
            iconLeftSpace += canvasWidth / utilities.getNumberOfIconsHorizontally();
            if (iconLeftSpace >= canvasWidth) {
                boundary = iconLeftSpace;
                iconLeftSpace = 5;
                iconTopSpace += (canvasHeight / utilities.getNumberOfIconsVertically());
            }

            if (isPassIcon(bitmapArrayList.get(i))) {
                if (iconLeftSpace <= 5) {
                    passIconsXList.add(boundary - (utilities.getIconWidth() / 2));
                    passIconsYList.add(iconTopSpace - (canvasHeight / utilities.getNumberOfIconsVertically()) + (utilities.getIconHeight() / 2));
                } else {
                    passIconsXList.add(iconLeftSpace - (utilities.getIconWidth() / 2));
                    passIconsYList.add(iconTopSpace + (utilities.getIconHeight() / 2));
                }
            }
        }
    }

    private void createConvexHull() {
        List<Point> points = GrahamScan.getConvexHull(passIconsXList, passIconsYList);

        points.remove(points.size() - 1);

        for (int i = 0; i < passIconsYList.size(); i++) {
            boolean flag = false;
            for (int j = 0; j < points.size(); j++) {
                int X = points.get(j).x;
                int Y = points.get(j).y;
                if (passIconsXList.get(i) == X && passIconsYList.get(i) == Y) {
                    flag = true;
                }
            }
            if (!flag) {
                points.add(new Point(passIconsXList.get(i), passIconsYList.get(i)));
            }
        }

        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            passIconsXList.set(i, point.x);
            passIconsYList.set(i, point.y);
        }
        System.out.println("X1: " + passIconsXList + " Y1:" + passIconsYList);
    }

    private void drawPolygon(Paint paint, Canvas canvas) {
        path.reset();
        path.moveTo(passIconsXList.get(0), passIconsYList.get(0));
        for (int i = 1; i < passIconsYList.size(); i++) {
            path.lineTo(passIconsXList.get(i), passIconsYList.get(i));
        }
        path.lineTo(passIconsXList.get(0), passIconsYList.get(0));

        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5);
        canvas.drawPath(path, paint);
    }

    @SuppressLint("NewApi")
    private boolean isPassIcon(Bitmap bitmap) {
        for (Bitmap SelectedPassIcon : selectedPassIcons) {
            if (SelectedPassIcon.sameAs(bitmap)) {
                return true;
            }
        }
        return false;
    }

    private Bitmap bitmapFromAssets(InputStream inputStream) {
        return Bitmap.createScaledBitmap(BitmapFactory.decodeStream(new BufferedInputStream(inputStream)),
                utilities.getIconWidth(), utilities.getIconHeight(), false);
    }

    private Bitmap convertUriToBitmap(Uri id) {
        Bitmap bitmap = null;
        try {
            bitmap = Bitmap.createScaledBitmap(BitmapFactory.decodeStream(context.getContentResolver().openInputStream(id)),
                    utilities.getIconWidth(), utilities.getIconHeight(), false);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private Bitmap getFilenameFromAssets(String filename) {
        AssetManager assetManager = context.getAssets();
        Bitmap bitmap = null;
        try {
            bitmap = bitmapFromAssets(assetManager.open("icons/" + filename));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bitmap;
    }

    private void UnlockPhone() {
        SlideActivity.btnUnlock.performClick();
    }

    @Override
    @SuppressLint("NewApi")
    public boolean onTouch(View v, MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN && v == surfaceView) {
            RectF pBounds = new RectF();
            float x = event.getX();
            float y = event.getY();
            path.computeBounds(pBounds, true);
            if (pBounds.contains(x, y)) {
                if (draw == utilities.getTotalRounds()) {
//                    MainLockScreenWindow.opensettings = true;
                    SlideActivity.removeview = true;
                    UnlockPhone();
                    switch (intentchooser) {
                        case 0:
                            Intent showCallLog = new Intent();
                            showCallLog.setAction(Intent.ACTION_VIEW);
                            showCallLog.setType(CallLog.Calls.CONTENT_TYPE);
                            context.startActivity(showCallLog);
                            break;
                        case 1:
                            Intent intent = new Intent("android.media.action.IMAGE_CAPTURE");
                            context.startActivity(intent);
                            break;
                        case 2:
                            Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                            smsIntent.addCategory(Intent.CATEGORY_DEFAULT);
                            smsIntent.setType("vnd.android-dir/mms-sms");
                            context.startActivity(smsIntent);
                            break;
                    }
                } else {
                    draw++;
                    draw();
                }
                return true;
            } else {
                if (wrongTries == 3) {
                    vibrator.vibrate(2000);
                    Toast.makeText(context, "Incorrect: Try Again", Toast.LENGTH_LONG).show();
                    draw = 1;
                    wrongTries = 1;
                    draw();
                } else {
                    wrongTries++;
                    draw();
                }
            }
        }
        vibrator.cancel();
        return false;
    }
}
