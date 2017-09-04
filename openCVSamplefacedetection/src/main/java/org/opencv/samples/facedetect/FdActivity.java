package org.opencv.samples.facedetect;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

public class FdActivity extends Activity implements CvCameraViewListener2 {

    private static final String TAG = "FdActivity";
    private static final Scalar FACE_RECT_COLOR = new Scalar(0, 255, 0, 255);
    public static final int JAVA_DETECTOR = 0;
    public static final int NATIVE_DETECTOR = 1;
    private Mat mRgba;
    private Mat mGray;
    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;
    private DetectionBasedTracker mNativeDetector;
    private int mDetectorType = JAVA_DETECTOR;
    private String[] mDetectorName;
    private float mRelativeFaceSize = 0.2f;
    private int mAbsoluteFaceSize = 0;
    public final static int FLAG_REGISTER = 0;
    public final static int FLAG_VERIFY = 1;
    private List<UserInfo> mUsers;
    private FaceMatcher matcher;
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            Intent intent;
            switch (msg.what) {
                case FLAG_REGISTER:
                    if (mFace == null) {
                        mFace = (Bitmap) msg.obj;
                        int result = matcher.histogramMatch(mFace);
                        if (result == matcher.UNFINISHED) {
                            mFace = null;
                        } else if (result == matcher.NO_MATCHER) {
                            intent = new Intent(FdActivity.this,
                                    RegisterActivity.class);
                            intent.putExtra("Face", mFace);
                            startActivity(intent);
                            finish();
                        } else {
                            intent = new Intent();
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    }
                    break;
                case FLAG_VERIFY:
                    if (mFace == null) {
                        mFace = (Bitmap) msg.obj;
                        int result = matcher.histogramMatch(mFace);
                        if (result == matcher.UNFINISHED) {
                            mFace = null;
                        } else if (result == matcher.NO_MATCHER) {
                            intent = new Intent();
                            setResult(RESULT_CANCELED, intent);
                            finish();
                        } else {
                            Log.e(TAG, "handleMessage: " + result);
                            intent = new Intent();
                            intent.putExtra("USER_ID", result);
                            setResult(RESULT_OK, intent);
                            finish();
                        }
                    }
                    break;
                default:
                    break;
            }
        }
    };

    private CameraBridgeViewBase mOpenCvCameraView;

    static {
        System.loadLibrary("opencv_java");
    }

    private View view;
    private ImageView ivScanLine;
    private Bitmap mFace;

    public FdActivity() {
        mDetectorName = new String[2];
        mDetectorName[JAVA_DETECTOR] = "Java";
        mDetectorName[NATIVE_DETECTOR] = "Native (tracking)";

        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "called onCreate");
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.face_detect_surface_view);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.fd_activity_surface_view);
        mOpenCvCameraView.setCvCameraViewListener(this);
        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        // 扫描动画
        view = findViewById(R.id.view);
        ivScanLine = (ImageView) findViewById(R.id.iv_scan_line);
        // 初始化匹配
        DatabaseHelper helper = new DatabaseHelper(this);
        mUsers = helper.query();
        matcher = new FaceMatcher(mUsers);

        // Load native library after(!) OpenCV initialization
        System.loadLibrary("detection_based_tracker");

        try {
            // load cascade file from application resources
            InputStream is = getResources().openRawResource(R.raw.lbpcascade_frontalface);
            File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
            mCascadeFile = new File(cascadeDir, "lbpcascade_frontalface.xml");
            FileOutputStream os = new FileOutputStream(mCascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            is.close();
            os.close();

            mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
            if (mJavaDetector.empty()) {
                Log.e(TAG, "Failed to load cascade classifier");
                mJavaDetector = null;
            } else
                Log.i(TAG, "Loaded cascade classifier from " + mCascadeFile.getAbsolutePath());

            mNativeDetector = new DetectionBasedTracker(mCascadeFile.getAbsolutePath(), 0);

            cascadeDir.delete();

        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Failed to load cascade. Exception thrown: " + e);
        }

        mOpenCvCameraView.enableView();
    }

    // 设置扫描动画
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);

        int[] location = new int[2];
        view.getLocationInWindow(location);
        int left = view.getLeft();
        int right = view.getRight();
        int top = view.getTop();
        int bottom = view.getBottom();
        Log.d(TAG, "onWindowFocusChanged: " + left + " " + right + " "
                + top + " " + bottom);

        Animation animation = new TranslateAnimation(left, left, top, bottom);
        animation.setDuration(3000);
        animation.setRepeatCount(Animation.INFINITE);

        ivScanLine.setAnimation(animation);
        animation.startNow();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mOpenCvCameraView != null)
            mOpenCvCameraView.disableView();
    }

    public void onDestroy() {
        super.onDestroy();
        mOpenCvCameraView.disableView();
    }

    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public Mat onCameraFrame(CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();
        Log.e(TAG, "mGray" + mGray.width() + ":" + mGray.height());
        // 翻转矩阵以适应前置摄像头
        Core.flip(mRgba, mRgba, 1);
        Core.flip(mGray, mGray, 1);
        // 控制检测矩阵区域和大小
        Rect rect = new Rect(
                new Point(mGray.width() / 2 - 300, mGray.height() / 2 - 300),
                new Size(600, 600));
        Core.rectangle(mRgba, rect.tl(), rect.br(), FACE_RECT_COLOR);
        Log.d(TAG, "onCameraFrame: " + rect.toString());
        mGray = new Mat(mGray, rect);

        if (mAbsoluteFaceSize == 0) {
            int height = mGray.rows();
            if (Math.round(height * mRelativeFaceSize) > 0) {
                mAbsoluteFaceSize = Math.round(height * mRelativeFaceSize);
            }
            mNativeDetector.setMinFaceSize(mAbsoluteFaceSize);
        }

        MatOfRect faces = new MatOfRect();

        if (mDetectorType == JAVA_DETECTOR) {
            if (mJavaDetector != null)
                mJavaDetector.detectMultiScale(mGray, faces, 1.1, 2, 2,
                        new Size(mAbsoluteFaceSize, mAbsoluteFaceSize), new Size());
        } else if (mDetectorType == NATIVE_DETECTOR) {
            if (mNativeDetector != null)
                mNativeDetector.detect(mGray, faces);
        } else {
            Log.e(TAG, "Detection method is not selected!");
        }

        Rect[] facesArray = faces.toArray();
        for (int i = 0; i < facesArray.length; i++) {
            // 需考虑屏幕适配, 1440*1080
            Point point = new Point(facesArray[i].x + 420, facesArray[i].y + 220);
            facesArray[i] = new Rect(point, facesArray[i].size());
            if (facesArray[i].height > 400 && facesArray[i].height < 500) {
                Core.rectangle(mRgba, facesArray[i].tl(), facesArray[i].br(),
                        FACE_RECT_COLOR, 3);
                Mat faceMat = new Mat(mRgba, facesArray[i]);
                Imgproc.resize(faceMat, faceMat, new Size(320, 320));
                Bitmap bitmap = Bitmap.createBitmap(faceMat.width(),
                        faceMat.height(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(faceMat, bitmap);
                Message message = Message.obtain();
                message.what = getIntent().getIntExtra("flag", -1);
                message.obj = bitmap;
                mHandler.sendMessage(message);
            }
        }
        return mRgba;
    }
}
