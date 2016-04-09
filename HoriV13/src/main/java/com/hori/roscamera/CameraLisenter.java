package com.hori.roscamera;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import android.hardware.Camera;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.Camera.PictureCallback;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.MediaRecorder;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;

public class CameraLisenter implements SurfaceHolder.Callback, Camera.PreviewCallback {

	private final static String TAG = "CameraLisenter";
	private final static boolean D = true;
	
	private boolean mIsInit;
	
	private int mFrameWidth;
	private int mFrameHeight;
	private int mFrameFormat;
	
	private Camera mCamera;
	private int mCameraId;
	private SurfaceHolder mSurfaceHolder;
	private MediaRecorder mMediaRecorder;
	
	private Handler				mHandler;
	
	private OutputStream		mOutputStream;
	
	private VideoStreamConnectThread mConnectThread;
	
	private PictureCallback mJpegCallback = new PictureCallback() {

		@Override
		public void onPictureTaken(byte[] data, Camera camera) {
			String dirName = Environment.getExternalStorageDirectory() + "/AutoMedia";
			File dir = new File(dirName);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			
			String fileName = System.currentTimeMillis() + ".jpg";
			File imgFile = new File(dirName, fileName);
			boolean isCapture = false;

			try {
				FileOutputStream fos = new FileOutputStream(imgFile);
				fos.write(data);
				fos.close();
				isCapture = true;
			} catch (Exception e) {
				e.printStackTrace();
				Log.w(TAG, "����ͼƬʧ��");
			}
			
			try {
				mCamera.setPreviewDisplay(mSurfaceHolder);
			} catch (IOException e) {
				e.printStackTrace();
			}
            // mCamera.setPreviewCallback(CameraLisenter.this);
            // mCamera.setDisplayOrientation(90);
            mCamera.startPreview();
            
            /*if (isCapture) {
            	broadcastUpdate(Constants.HM_CAPTURE_IMAGE, null, 1);
            } else {
            	broadcastUpdate(Constants.HM_CAPTURE_IMAGE, null, 0);
            }*/
		}
		
	};
	
	public CameraLisenter(Handler handler) {
		mHandler = handler;
		mIsInit = false;

		mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
		mCamera = Camera.open(mCameraId);
		if (mCamera == null) {
			if (D)	Log.w(TAG, "Open Camera " + mCameraId + "Fail!");
			return;
		}
		
        Camera.Parameters parameters = mCamera.getParameters();
        // parameters.setPreviewSize(mFrameWidth, mFrameHeight);
        android.hardware.Camera.Size size = parameters.getPreviewSize();
        mFrameWidth = size.width;
        mFrameHeight = size.height;
        mFrameFormat = parameters.getPreviewFormat();
        
		mConnectThread = new VideoStreamConnectThread();
		mConnectThread.start();

	}
	
	public void cancel() {
		if (null != mConnectThread) {
			mConnectThread.cancel();
			mConnectThread = null;
		}
		
		try{
	        if (mCamera != null) {
	        	mCamera.setPreviewCallback(null);
	            mCamera.stopPreview();
	            mCamera.release();
	            mCamera = null;
	        } 
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	public void switchCamera() {
		if (mCameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
			mCameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
		} else {
			mCameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
		}
		
		if (mCamera != null) {
			try{
	        	mCamera.setPreviewCallback(null);
	            mCamera.stopPreview();
	            mCamera.release();
	            mCamera = null;
	        } catch (Exception e) {
	            e.printStackTrace();
	        }
		}
		
		mCamera = Camera.open(mCameraId);
		
		try {
			mCamera.setPreviewDisplay(mSurfaceHolder);
			mCamera.setPreviewCallback(this);
	        // mCamera.setDisplayOrientation(90);
			mCamera.startPreview();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void capturePhoto() {
		if (mCamera != null) {
			mCamera.autoFocus(new AutoFocusCallback() {
				@Override
				public void onAutoFocus(boolean success, Camera camera) {
					mCamera.takePicture(null, null, mJpegCallback);
				}
			});
		}
	}
	

	public void captureVideo(boolean flag) {
		if (flag) {	// ��ʼ
			if (mMediaRecorder != null) {
				mMediaRecorder.reset();
			} else {
				mMediaRecorder = new MediaRecorder();
			}
	        mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA); 
	        mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4); 
	        mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264); 
	        android.hardware.Camera.Size size = mCamera.getParameters().getPictureSize();
	        mMediaRecorder.setVideoSize(size.width, size.height); 
	        mMediaRecorder.setVideoFrameRate(20); 
	        mMediaRecorder.setPreviewDisplay(mSurfaceHolder.getSurface()); 
	        String dirName = Environment.getExternalStorageDirectory() + "/AutoMedia";
			File dir = new File(dirName);
			if (!dir.exists()) {
				dir.mkdirs();
			}
			
			String fileName = System.currentTimeMillis() + ".mp4";
	        mMediaRecorder.setOutputFile(dirName + "/" + fileName);
	        
	        try { 
	            mMediaRecorder.prepare(); 
	            mMediaRecorder.start(); 
	        } catch (Exception e) {
	        	e.printStackTrace();
	        	Log.w(TAG, "¼��ʧ��");
	        }
		} else {
			if (mMediaRecorder != null) { 
	            mMediaRecorder.stop(); 
	            mMediaRecorder.release(); 
	            mMediaRecorder = null;
	        }
		}
	}
	
	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		mSurfaceHolder = holder;
		/*try {
            if (mCamera != null) {
                mCamera.setPreviewDisplay(holder);
                mCamera.startPreview();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }*/
	}
	
	@Override
	public void onPreviewFrame(byte[] data, Camera camera) {
		if ((!mIsInit) || (null == data)) {	
			Log.w(TAG, "UnInit");
			return ;
		}
		if (null == mOutputStream) {
			Log.w(TAG, "UnConnection");
			return ;
		}
		
		YuvImage image = new YuvImage(data, mFrameFormat, mFrameWidth, mFrameHeight, null);
		try {
			/*if (*/image.compressToJpeg(new Rect(0, 0, mFrameWidth, mFrameHeight), 50, mOutputStream);//) {
				mOutputStream.flush();
			// }
		} catch (Exception e) {
			e.printStackTrace();
			// mConnectThread.unLockDisconnectLock();
		}
	}
	
	@Override
	public void surfaceChanged(SurfaceHolder holder, int format, int width,
			int height) {
		mSurfaceHolder = holder;
		if (mCamera == null) {
            return;
        }
        mCamera.stopPreview();
        try {
			mCamera.setPreviewDisplay(holder);
		} catch (IOException e) {
			e.printStackTrace();
		}
        mCamera.setPreviewCallback(this);
        // mCamera.setDisplayOrientation(90);

        Camera.Parameters parameters = mCamera.getParameters();
        android.hardware.Camera.Size size = parameters.getPreviewSize();
        mFrameWidth = size.width;
        mFrameHeight = size.height;
        mFrameFormat = parameters.getPreviewFormat();
        mCamera.startPreview();
		
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		try{
	        if (mCamera != null) {
	        	mCamera.setPreviewCallback(null);
	            mCamera.stopPreview();
	            mCamera.release();
	            mCamera = null;
	        } 
        } catch (Exception e) {
            e.printStackTrace();
        } 
		
	}

	private void broadcastUpdate(int what, Object obj) {
		Message msg = new Message();
		msg.what = what;
		msg.obj = obj;
		
		mHandler.sendMessage(msg);
	}

	private class VideoStreamConnectThread extends Thread {
		private final static String TAG = "VideoStreamConnectThread";
		public static final int STRM_PORT = 6666;
		
		private boolean				mmIsRun;
		
		private ServerSocket		mmServerSocket;
		private Socket				mmClientSocket;
		
		private Lock mmDisconnectLock;
		private Condition mmDisconnectCondition;
		
		public void unLockDisconnectLock() {
			if (mOutputStream != null) {
				try {
					mOutputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					mOutputStream = null;
				}
			}
			
			mmDisconnectLock.lock();
			mmDisconnectCondition.signalAll();
			mmDisconnectLock.unlock();
			
			mIsInit = false;
		}
		
		@Override
		public void run() {
			if (!initialize()) {
				if (D)	Log.i(TAG, "Init Fail!");
				cancel();
				return;
			}
			while (mmIsRun) {
				// clear();
				try {
					if (D) Log.i(TAG, "Start accept");
					// broadcastUpdate(0, null);
					mmClientSocket = mmServerSocket.accept();
				} catch (IOException e) {
					e.printStackTrace();
					if (D)	Log.i(TAG, "accept Fail!");
					continue;
				}
				
				if (D)	Log.i(TAG, "accept Success!");
				
				try {
					mOutputStream = mmClientSocket.getOutputStream();
				} catch (IOException e) {
					e.printStackTrace();
					if (D)	Log.i(TAG, "Get OutputStream Fail!");
					continue;
				}

				mIsInit = true;
				
				if (D) Log.i(TAG, "Got OutputStream");
				broadcastUpdate(1, null);
				/*mmDisconnectLock.lock();
				try {
					Log.i(TAG, "Start await");
					mmDisconnectCondition.await();
					broadcastUpdate(2, null);
				} catch (Exception e) {
					e.printStackTrace();
				}
				mmDisconnectLock.unlock();*/
			}
		}
		
		private void clear() {
			
			mIsInit = false;
			
			if (mOutputStream != null) {
				try {
					mOutputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					mOutputStream = null;
				}
			}
		}
		
		public void cancel() {
			unLockDisconnectLock();	// UnLock
			
			mmIsRun = false;

			if (mOutputStream != null) {
				try {
					mOutputStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				} finally {
					mOutputStream = null;
				}
			}
			if (mmServerSocket != null) {
				try {
					mmServerSocket.close();
	    		} catch (IOException e) {
	    			e.printStackTrace();
	    		} finally {
	    			mmServerSocket = null;
	    		}
			}

			if (D)	Log.i(TAG, "ConnectThread ȡ��, �Ͽ�����");
		}
		
		private boolean initialize() {
			try {
				mmServerSocket = new ServerSocket(STRM_PORT);
			} catch (IOException e) {
				e.printStackTrace();
				if (D)	Log.i(TAG, "ServerSocket Fail!");
				mmIsRun = false;
				return false;
			}
			
			mmDisconnectLock = new ReentrantLock();
			mmDisconnectCondition = mmDisconnectLock.newCondition();
			
			mmIsRun = true;
			return true;
		}
		
	}





}
