package org.robin.activity;

import org.robin.camera.CameraInterface;
import org.robin.camera.CameraInterface.CamOpenOverCallback;
import org.robin.camera.preview.CameraSurfaceView;
import org.robin.classify.TensorFlowImageClassifier;
import org.robin.playcamera.R;
import org.robin.util.DisplayUtil;

import android.app.Activity;
import android.graphics.Point;
import android.os.Bundle;
import android.view.Menu;
import android.view.SurfaceHolder;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import org.robin.classify.Classifier;

public class CameraActivity extends Activity implements CamOpenOverCallback,CameraSurfaceView.OnSurfaceHolderLister {
	private static final String TAG = "TFrobin_activity";
	CameraSurfaceView surfaceView = null;
	ImageButton shutterBtn,camerapickerBtn;
	float previewRate = -1f;
	private static final int INPUT_SIZE = 224;
	private static final int IMAGE_MEAN = 128;
	private static final float IMAGE_STD = 1;
	private static final String INPUT_NAME =  "input";
	private static final String OUTPUT_NAME = "final_result";


	private static final String MODEL_FILE_REAR = "file:///android_asset/bluesky_graph.pb";
	private static final String LABEL_FILE_REAR = "file:///android_asset/bluesky_labels.txt";

	private static final String MODEL_FILE_YEAP = "file:///android_asset/graph.pb";
	private static final String LABEL_FILE_YEAP = "file:///android_asset/labels.txt";

	private Classifier classifier =null;
	private Executor singleThreadExecutor = Executors.newSingleThreadExecutor();
	private TextView tv;
	private int mCameraId = 0;// default rear

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		Thread openThread = new Thread(){
			@Override
			public void run() {
				// TODO Auto-generated method stub
				CameraInterface.getInstance().doOpenCamera(CameraActivity.this,mCameraId);
			}
		};
		openThread.start();
		initTensorFlow();
		setContentView(R.layout.activity_camera);
		initUI();
		initViewParams();
		
		shutterBtn.setOnClickListener(new BtnListeners());
		camerapickerBtn.setOnClickListener(new PickerListeners());
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.camera, menu);
		return true;
	}

	private void initUI(){
		surfaceView = (CameraSurfaceView)findViewById(R.id.camera_surfaceview);
        surfaceView.setHolderCallback(this);
		shutterBtn = (ImageButton)findViewById(R.id.btn_shutter);
		camerapickerBtn =(ImageButton)findViewById(R.id.btn_camerapicker);

		tv = (TextView) findViewById(R.id.text_result);
	}

	public void showText(final String msg) {
		tv.post(new Runnable() {
			@Override
			public void run() {
				tv.setText(msg);
			}
		});
	}
	private void initViewParams(){
		LayoutParams params = surfaceView.getLayoutParams();
		Point p = DisplayUtil.getScreenMetrics(this);
		params.width = p.x;
		params.height = p.y;
		previewRate = DisplayUtil.getScreenRate(this);
		surfaceView.setLayoutParams(params);

		LayoutParams p2 = shutterBtn.getLayoutParams();
		p2.width = DisplayUtil.dip2px(this, 80);
		p2.height = DisplayUtil.dip2px(this, 80);;		
		shutterBtn.setLayoutParams(p2);	

	}

	@Override
	public void cameraHasOpened() {
		// TODO Auto-generated method stub
		SurfaceHolder holder = surfaceView.getSurfaceHolder();
		CameraInterface.getInstance().doStartPreview(holder, previewRate);
		CameraInterface.getInstance().setTv(tv);
	}

	private class BtnListeners implements OnClickListener{

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch(v.getId()){
			case R.id.btn_shutter:
				CameraInterface.getInstance().doTakePicture();
				break;
			default:break;
			}
		}
	}

	private class PickerListeners implements OnClickListener {
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch(v.getId()){
				case R.id.btn_camerapicker:
				{
					if(mCameraId == 0){
						mCameraId = 1;
						initTensorFlow();
					}else{
						mCameraId = 0;
						initTensorFlow();
					}
					CameraInterface.getInstance().change();
					cameraHasOpened();
					break;
				}

				default:break;
			}
		}
	}

    @Override
    public void surfaceHolderCreate(SurfaceHolder holder){
		while(CameraInterface.getInstance().mCamera == null);
        CameraInterface.getInstance().SetSurfaceHolder(holder);
    }

	@Override
	protected void onDestroy() {
		super.onDestroy();
		singleThreadExecutor.execute(new Runnable() {
			@Override
			public void run() {
				classifier.close();
			}
		});
//		classifier.close();
	}

	private void initTensorFlow() {
		singleThreadExecutor.execute(new Runnable() {
			@Override
			public void run() {
				try {
					if(true){//(mCameraId == 0){
						classifier = TensorFlowImageClassifier.create(
								getAssets(),
								MODEL_FILE_REAR,
								LABEL_FILE_REAR,
								INPUT_SIZE,
								IMAGE_MEAN,
								IMAGE_STD,
								INPUT_NAME,
								OUTPUT_NAME);
//					classifier.enableStatLogging(true);
					}else{
						classifier = TensorFlowImageClassifier.create(
								getAssets(),
								MODEL_FILE_YEAP,
								LABEL_FILE_YEAP,
								INPUT_SIZE,
								IMAGE_MEAN,
								IMAGE_STD,
								INPUT_NAME,
								OUTPUT_NAME);
//					classifier.enableStatLogging(true);
					}
				} catch (final Exception e) {
				throw new RuntimeException("Error initializing TensorFlow!", e);
				}
				CameraInterface.getInstance().setClassifier(classifier);
			}

		});
	}
}
