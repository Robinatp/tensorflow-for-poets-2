package org.robin.camera;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.robin.classify.Classifier;
import org.robin.classify.TensorFlowImageClassifier;
import org.robin.util.CamParaUtil;
import org.robin.util.FileUtil;
import org.robin.util.ImageUtil;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.PreviewCallback;
import android.hardware.Camera.ShutterCallback;
import android.hardware.Camera.Size;
import android.os.Environment;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.TextView;

public class CameraInterface implements PreviewCallback{
	private static final String TAG = "TFrobin_CameraInterface";
	public Camera mCamera;
	private Camera.Parameters mParams;
	private boolean isPreviewing = false;
    private Bitmap currentTakeBitmap =null;
    private Bitmap currentPreviewBitmap =null;
	private float mPreviwRate = -1f;
    private String old_classfier_result ="other";
    private String new_classfier_result = null;
	private static CameraInterface mCameraInterface;
	private static final int INPUT_SIZE = 224;
	private Classifier classifier;
    private Executor singleThreadExecutor = Executors.newSingleThreadExecutor();
	private int mCameraId = 0;// default rear
	private int morientation = 0;

	private Size previewSize;
	private Size pictureSize;
	private TextView tv;
	
	public Size getPictureSize() {
		return pictureSize;
	}
	public Size getPreviewSize() {
		return previewSize;
	}
	public void setClassifier(Classifier classifier) {
		this.classifier = classifier;
	}

	public void setTv(TextView tv) {
		this.tv = tv;
	}

	public interface CamOpenOverCallback{
		public void cameraHasOpened();
	}

	private CameraInterface(){

	}
	public static synchronized CameraInterface getInstance(){
		if(mCameraInterface == null){
			mCameraInterface = new CameraInterface();
		}
		return mCameraInterface;
	}
	/**打开Camera
	 * @param callback
	 */
	public void doOpenCamera(CamOpenOverCallback callback,int cameraId){
		Log.i(TAG, "Camera open....");
		mCameraId = cameraId;
		if(mCameraId ==0){
			mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
		}else{
			mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
		}
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		Camera.getCameraInfo(mCameraId, cameraInfo);
		morientation = cameraInfo.orientation;

		Log.i(TAG, "Camera open over....");
		callback.cameraHasOpened();
//		mCamera.setPreviewCallback(this);
	}

	public void change() {
		if (mCameraId == 0) {

			//if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
//			mCamera.stopPreview();
//			mCamera.release();
//			mCamera = null;
			doStopCamera();
			mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_FRONT);
//			try {
//				mCamera.setPreviewDisplay(mHolder);
//			} catch (IOException e) {
//				e.printStackTrace();
//			}
//			mCamera.startPreview();
			mCameraId = 1;
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			Camera.getCameraInfo(mCameraId, cameraInfo);
			morientation = cameraInfo.orientation;
			Log.i(TAG, "Camerachange....mCameraId:"+mCameraId+"   facing:"+cameraInfo.facing+"  orientation:"+morientation);
			//}
		} else {
			//if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
//			mCamera.stopPreview();
//			mCamera.release();
//			mCamera = null;
			doStopCamera();
			mCamera = Camera.open(Camera.CameraInfo.CAMERA_FACING_BACK);
//			try {
//				mCamera.setPreviewDisplay(mHolder);
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//			mCamera.startPreview();
			mCameraId = 0;
			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
			Camera.getCameraInfo(mCameraId, cameraInfo);
			morientation = cameraInfo.orientation;
			Log.i(TAG, "Camerachange....mCameraId:"+mCameraId+"   facing:"+cameraInfo.facing+"  orientation:"+morientation);
			//}
		}

	}

	/**开启预览
	 * @param holder
	 * @param previewRate
	 */
	public void doStartPreview(SurfaceHolder holder, float previewRate){
		Log.i(TAG, "doStartPreview... previewRate ="+previewRate);
		if(isPreviewing){
			mCamera.stopPreview();
			return;
		}
		if(mCamera != null){

			mCamera.setPreviewCallback(this);

			mParams = mCamera.getParameters();
			mParams.setPictureFormat(PixelFormat.JPEG);//设置拍照后存储的图片格式
			CamParaUtil.getInstance().printSupportPictureSize(mParams);
			CamParaUtil.getInstance().printSupportPreviewSize(mParams);
			//设置PreviewSize和PictureSize
			pictureSize = CamParaUtil.getInstance().getPropPictureSize(
					mParams.getSupportedPictureSizes(),previewRate, 4032);
			mParams.setPictureSize(pictureSize.width, pictureSize.height);
			previewSize = CamParaUtil.getInstance().getPropPreviewSize(
					mParams.getSupportedPreviewSizes(), previewRate, 1920);
			mParams.setPreviewSize(previewSize.width, previewSize.height);

			Log.i(TAG, "PreviewSize--With = " + previewSize.width
					+ "Height = " + previewSize.height);
			Log.i(TAG, "PictureSize--With = " + pictureSize.width
					+ "Height = " + pictureSize.height);
			
			mCamera.setDisplayOrientation(90);

			CamParaUtil.getInstance().printSupportFocusMode(mParams);
			List<String> focusModes = mParams.getSupportedFocusModes();
			if(focusModes.contains("continuous-picture")){
				mParams.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
//                mParams.setFocusMode("manual");
			}

			mParams.set("zsd-mode","on");
			mParams.set("mtk-cam-mode",1);
			mCamera.setParameters(mParams);	

			try {
				mCamera.setPreviewDisplay(holder);
				mCamera.startPreview();//开启预览
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			isPreviewing = true;
			mPreviwRate = previewRate;

			mParams = mCamera.getParameters(); //重新get一次
			Log.i(TAG, "Final:PreviewSize--With = " + mParams.getPreviewSize().width
					+ "Height = " + mParams.getPreviewSize().height);
			Log.i(TAG, "Final:PictureSize--With = " + mParams.getPictureSize().width
					+ "Height = " + mParams.getPictureSize().height);
		}
	}
	/**
	 * 停止预览，释放Camera
	 */
	public void doStopCamera(){
		if(null != mCamera)
		{
			mCamera.setPreviewCallback(null);
			mCamera.stopPreview(); 
			isPreviewing = false; 
			mPreviwRate = -1f;
			mCamera.release();
			mCamera = null;     
		}
	}
	/**
	 * 拍照
	 */
	public void doTakePicture(){
		if(isPreviewing && (mCamera != null)){
			mCamera.takePicture(mShutterCallback, null, mJpegPictureCallback);
		}
	}

	/*为了实现拍照的快门声音及拍照保存照片需要下面三个回调变量*/
	ShutterCallback mShutterCallback = new ShutterCallback() 
	//快门按下的回调，在这里我们可以设置类似播放“咔嚓”声之类的操作。默认的就是咔嚓。
	{
		public void onShutter() {
			// TODO Auto-generated method stub
			Log.i(TAG, "myShutterCallback:onShutter...");
		}
	};
	PictureCallback mRawCallback = new PictureCallback() 
	// 拍摄的未压缩原数据的回调,可以为null
	{

		public void onPictureTaken(byte[] data, Camera camera) {
			// TODO Auto-generated method stub
			Log.i(TAG, "myRawCallback:onPictureTaken...");

		}
	};
	PictureCallback mJpegPictureCallback = new PictureCallback() 
	//对jpeg图像数据的回调,最重要的一个回调
	{
		public void onPictureTaken(byte[] data, Camera camera) {
			// TODO Auto-generated method stub
			Log.i(TAG, "myJpegCallback:onPictureTaken...");

            if(null != data){
                currentTakeBitmap = BitmapFactory.decodeByteArray(data, 0, data.length);//data是字节数据，将其解析成位图
                isPreviewing = false;
            }else{
                return;
            }
            singleThreadExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    //保存图片到sdcard
                    if(null != currentTakeBitmap)
                    {
                        //设置FOCUS_MODE_CONTINUOUS_VIDEO)之后，myParam.set("rotation", 90)失效。
                        //图片竟然不能旋转了，故这里要旋转下
                        Bitmap rotaBitmap = ImageUtil.getRotateBitmap(currentTakeBitmap, morientation);
                        FileUtil.saveBitmap(rotaBitmap);
                    }
                }
            });
			isPreviewing = true;
		}
	};
	
	
	public void SetSurfaceHolder(SurfaceHolder holder){
		try {
			mCamera.setPreviewDisplay(holder);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public boolean checkParamChange(String old_value, String new_value){
        Log.i(TAG, "checkParamChange...:"+old_value.substring(1, 4)+"old_value:"+old_value);
        if((old_value.length()>5)&&(new_value.length()>5)&&!old_value.isEmpty() && !new_value.isEmpty())
            return old_value.substring(1, 4).equals(new_value.substring(1, 4));
        else
            return false;
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera c) {
		// TODO Auto-generated method stub
        Log.i(TAG, "onPreviewFrame....");

        if(null != data){
//            savePicture(data,c);
            currentPreviewBitmap =runInPreviewFrame(data,c);
        }else{
            return;
        }

        if(currentPreviewBitmap != null)
        {
            new_classfier_result = dealWithBitmap(currentPreviewBitmap);

            if(!checkParamChange(old_classfier_result,new_classfier_result))
            {
                if(new_classfier_result.contains("bluesky")){
                    mParams = mCamera.getParameters();
                    mParams.set("nv-process-mode","bluesky");
                    mCamera.setParameters(mParams);
                    Log.i(TAG, "checkParamChange().. bluesky");
                }
                else{
                    mParams = mCamera.getParameters();
                    mParams.set("nv-process-mode","none");
                    mCamera.setParameters(mParams);
                    Log.i(TAG, "checkParamChange().. none");
                }
            }
            old_classfier_result = new String(new_classfier_result);
        }

        singleThreadExecutor.execute(new Runnable() {
            @Override
            public void run() {
                showText(new_classfier_result);
            }
        });


	}

	public void showText(final String msg) {
		tv.post(new Runnable() {
			@Override
			public void run() {
				tv.setText(msg);
			}
		});
	}

	/**
	 * 处理图片，开始识别图片内容
	 *
	 * @param bitmap
	 */
	private String dealWithBitmap(Bitmap bitmap) {

		//TODO
        List<Classifier.Recognition> results;
		bitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, false);

        if(bitmap != null&& classifier != null)
		{
//			FileUtil.saveBitmap(bitmap);
			results = classifier.recognizeImage(bitmap);
		}

        else
            results =null;

		Log.e(TAG, "dealWithBitmap: " + String.valueOf(results));

		return String.valueOf(results);

//		showResult(results);
	}


    public Bitmap runInPreviewFrame(byte[] data, Camera camera) {
    	ByteArrayOutputStream baos;
        byte[] rawImage;
        Bitmap bitmap;
        //处理data
        Camera.Size previewSize = camera.getParameters().getPreviewSize();//获取尺寸,格式转换的时候要用到
        BitmapFactory.Options newOpts = new BitmapFactory.Options();
        newOpts.inJustDecodeBounds = true;
        YuvImage yuvimage = new YuvImage(
                data,
                camera.getParameters().getPreviewFormat(),//ImageFormat.NV21,
                previewSize.width,
                previewSize.height,
                null);
        baos = new ByteArrayOutputStream();
        yuvimage.compressToJpeg(new Rect(0, 0, previewSize.width, previewSize.height), 100, baos);// 80--JPG图片的质量[0-100],100最高
        rawImage = baos.toByteArray();
        //将rawImage转换成bitmap
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.RGB_565;
        bitmap = BitmapFactory.decodeByteArray(rawImage, 0, rawImage.length, options);
        //bitmap = ImageUtil.getRotateBitmap(bitmap, 90.0f);
        return ImageUtil.getRotateBitmap(bitmap, morientation);
    }
	
	
	public void savePicture(byte[] data, Camera c){
        String fileName = "IMG_"
				+ new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date())
						.toString() + ".jpg";
        
		final String root = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "PlayCamera_Frame";
		final File mkDir = new File(root);
		
		if (!mkDir.exists())
			mkDir.mkdirs();
		File pictureFile = new File(root, fileName);
		
		if (!pictureFile.exists()) {
			try {
				pictureFile.createNewFile();
				YuvImage image = new YuvImage(data,
		                c.getParameters().getPreviewFormat(), 
		                c.getParameters().getPreviewSize().width, 
		                c.getParameters().getPreviewSize().height,
						null);
				FileOutputStream filecon = new FileOutputStream(pictureFile);
				image.compressToJpeg(
						new Rect(0, 0, image.getWidth(), image.getHeight()),
						90, filecon);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

}
