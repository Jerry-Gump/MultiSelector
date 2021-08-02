package com.donkingliang.imageselector;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.core.os.EnvironmentCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.donkingliang.imageselector.adapter.VideoAdapter;
import com.donkingliang.imageselector.entry.FileData;
import com.donkingliang.imageselector.entry.Folder;
import com.donkingliang.imageselector.entry.RequestConfig;
import com.donkingliang.imageselector.model.BaseModel;
//import com.donkingliang.imageselector.model.ImageModel;
import com.donkingliang.imageselector.model.VideoModel;
import com.donkingliang.imageselector.utils.MultiSelector;
import com.donkingliang.imageselector.utils.ImageUtil;
import com.donkingliang.imageselector.utils.UriUtils;
import com.donkingliang.imageselector.utils.VersionUtils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class VideoSelectorActivity  extends BaseSelectorActivity {

    private static final int CAMERA_REQUEST_CODE = 0x00000010;
    private Uri mCameraUri;
    private String mCameraImagePath;
    private long mTakeTime;

    private boolean useCamera = true;
    private boolean onlyTakePhoto = false;

    private FrameLayout btnPreview;

    /**
     * 启动图片选择器
     *
     * @param activity
     * @param requestCode
     * @param config
     */
    public static void openActivity(Activity activity, int requestCode, RequestConfig config) {
        Intent intent = new Intent(activity, VideoSelectorActivity.class);
        intent.putExtra(MultiSelector.KEY_CONFIG, config);
        activity.startActivityForResult(intent, requestCode);
    }

    /**
     * 启动图片选择器
     *
     * @param fragment
     * @param requestCode
     * @param config
     */
    public static void openActivity(Fragment fragment, int requestCode, RequestConfig config) {
        Intent intent = new Intent(fragment.getActivity(), VideoSelectorActivity.class);
        intent.putExtra(MultiSelector.KEY_CONFIG, config);
        fragment.startActivityForResult(intent, requestCode);
    }

    /**
     * 启动图片选择器
     *
     * @param fragment
     * @param requestCode
     * @param config
     */
    public static void openActivity(android.app.Fragment fragment, int requestCode, RequestConfig config) {
        Intent intent = new Intent(fragment.getActivity(), VideoSelectorActivity.class);
        intent.putExtra(MultiSelector.KEY_CONFIG, config);
        fragment.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        RequestConfig config = intent.getParcelableExtra(MultiSelector.KEY_CONFIG);
        useCamera = config.useCamera;
        onlyTakePhoto = config.onlyTakePhoto;
        if (onlyTakePhoto) {
            // 仅拍照
            checkPermissionAndCamera();
        } else {
            setContentView(R.layout.activity_video_select);
            setStatusBarColor();
            initView();
            initListener();
            initVideoList();
            checkPermissionAndLoadFiles();
            hideFolderList();
            setSelectFileCount(0);
        }
    }

    @Override
    protected void initView() {
        super.initView();
        btnPreview = findViewById(R.id.btn_preview);
    }

    @Override
    protected void initListener() {
        super.initListener();
        btnPreview.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                ArrayList<FileData> fileData = new ArrayList<>();
                fileData.addAll(mAdapter.getSelectFiles());
                if(fileData.size()>0) {
                    openVideoFile(fileData.get(0));
                }
            }
        });
    }

    private void openVideoFile(FileData fileData){
        Intent intent=new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        //SDCard卡根目录下/DCIM/Camera/test.mp4文件
        Uri uri= fileData.getUri();//Uri.parse("file:///sdcard/DCIM/Camera/test.mp4");
        intent.setDataAndType(uri, "video/*");
        startActivity(intent);
    }

    /**
     * 初始化图片列表
     */
    private void initVideoList() {
        // 判断屏幕方向
        Configuration configuration = getResources().getConfiguration();
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            mLayoutManager = new GridLayoutManager(this, 3);
        } else {
            mLayoutManager = new GridLayoutManager(this, 5);
        }

        rvImage.setLayoutManager(mLayoutManager);
        mAdapter = new VideoAdapter(this, mMaxCount, isSingle, canPreview);
        rvImage.setAdapter(mAdapter);
        ((SimpleItemAnimator) rvImage.getItemAnimator()).setSupportsChangeAnimations(false);
        if (mFolders != null && !mFolders.isEmpty()) {
            setFolder(mFolders.get(0));
        }
        mAdapter.setOnFileSelectListener(new VideoAdapter.OnFileSelectListener() {
            @Override
            public void OnFileSelect(FileData fileData, boolean isSelect, int selectCount) {
                setSelectFileCount(selectCount);
            }
        });
        mAdapter.setOnItemClickListener(new VideoAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(FileData fileData, int position) {
                // 视频暂时不考虑预览，而应该尝试启用第三方播放应用，因为这个播放多种格式视频需要用到ffmpeg，规模太大了，不适应这个轻量化的思路
                //toPreviewActivity(mAdapter.getData(), position);
                if(fileData != null){
                    openVideoFile(fileData);
                }
            }

            @Override
            public void OnCameraClick() {
                checkPermissionAndCamera();
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (applyLoadFile) {
            applyLoadFile = false;
            checkPermissionAndLoadFiles();
        }
        if (applyCamera) {
            applyCamera = false;
            checkPermissionAndCamera();
        }
    }

    /**
     * 处理图片预览页返回的结果
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == MultiSelector.RESULT_CODE) {
            if (data != null && data.getBooleanExtra(MultiSelector.IS_CONFIRM, false)) {
                //如果用户在预览页点击了确定，就直接把用户选中的图片返回给用户。
                confirm();
            } else {
                //否则，就刷新当前页面。
                mAdapter.notifyDataSetChanged();
                setSelectFileCount(mAdapter.getSelectFiles().size());
            }
        } else if (requestCode == CAMERA_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                ArrayList<String> images = new ArrayList<>();
                Uri savePictureUri = null;
                if (VersionUtils.isAndroidQ()) {
                    savePictureUri = mCameraUri;
                    images.add(UriUtils.getPathForUri(this, mCameraUri));
                } else {
                    savePictureUri = Uri.fromFile(new File(mCameraImagePath));
                    images.add(mCameraImagePath);
                }
                ImageUtil.savePicture(this,savePictureUri,mTakeTime);
                saveFileAndFinish(images, true);
            } else {
                if (onlyTakePhoto) {
                    finish();
                }
            }
        }
    }

    /**
     * 检查权限并拍照。
     */
    private void checkPermissionAndCamera() {
        int hasCameraPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
        int hasWriteExternalPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasCameraPermission == PackageManager.PERMISSION_GRANTED
                && hasWriteExternalPermission == PackageManager.PERMISSION_GRANTED) {
            //有调起相机拍照。
            openCamera();
        } else {
            //没有权限，申请权限。
            ActivityCompat.requestPermissions(VideoSelectorActivity.this,
                    new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_CAMERA_REQUEST_CODE);
        }
    }

    /**
     * 处理权限申请的回调。
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_WRITE_EXTERNAL_REQUEST_CODE) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //允许权限，加载图片。
                loadFileForSDCard();
            } else {
                //拒绝权限，弹出提示框。
                showExceptionDialog(true, getString(R.string.selector_video_permissions_hint));
            }
        } else if (requestCode == PERMISSION_CAMERA_REQUEST_CODE) {
            if (grantResults.length > 1
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED
                    && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                //允许权限，有调起相机拍照。
                openCamera();
            } else {
                //拒绝权限，弹出提示框。
                showExceptionDialog(false, getString(R.string.selector_video_permissions_hint));
            }
        }
    }

    /**
     * 从SDCard加载图片。
     */
    protected void loadFileForSDCard() {
        VideoModel.loadVideoForSDCard(this, new BaseModel.DataCallback() {
            @Override
            public void onSuccess(ArrayList<Folder> folders) {
                mFolders = folders;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mFolders != null && !mFolders.isEmpty()) {
                            initFolderList(R.string.selector_file_num);
                            mFolders.get(0).setUseCamera(useCamera);
                            setFolder(mFolders.get(0));
                            if (mSelectedFiles != null && mAdapter != null) {
                                mAdapter.setSelectedFiles(mSelectedFiles);
                                mSelectedFiles = null;
                                setSelectFileCount(mAdapter.getSelectFiles().size());
                            }
                        }
                    }
                });
            }
        });
    }

    /**
     * 调起相机拍照
     */
    private void openCamera() {
        Intent captureIntent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
        if (captureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            Uri photoUri = null;

            if (VersionUtils.isAndroidQ()) {
                photoUri = createVideoPathUri();
            } else {
                try {
                    photoFile = createVideoFile();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (photoFile != null) {
                    mCameraImagePath = photoFile.getAbsolutePath();
                    if (VersionUtils.isAndroidN()) {
                        //通过FileProvider创建一个content类型的Uri
                        photoUri = FileProvider.getUriForFile(this, getPackageName() + ".imageSelectorProvider", photoFile);
                    } else {
                        photoUri = Uri.fromFile(photoFile);
                    }
                }
            }

            mCameraUri = photoUri;
            if (photoUri != null) {
                captureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                captureIntent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                startActivityForResult(captureIntent, CAMERA_REQUEST_CODE);
                mTakeTime = System.currentTimeMillis();
            }
        }
    }

    /**
     * 创建一条图片地址uri,用于保存拍照后的照片
     *
     * @return 图片的uri
     */
    public Uri createVideoPathUri() {
        String status = Environment.getExternalStorageState();
        SimpleDateFormat timeFormatter = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault());
        long time = System.currentTimeMillis();
        String imageName = timeFormatter.format(new Date(time));
        // ContentValues是我们希望这条记录被创建时包含的数据信息
        ContentValues values = new ContentValues(2);
        values.put(MediaStore.Video.Media.DISPLAY_NAME, imageName);
        values.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
        // 判断是否有SD卡,优先使用SD卡存储,当没有SD卡时使用手机存储
        if (status.equals(Environment.MEDIA_MOUNTED)) {
            return getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values);
        } else {
            return getContentResolver().insert(MediaStore.Video.Media.INTERNAL_CONTENT_URI, values);
        }
    }

    private File createVideoFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = String.format("VID_%s.mp4", timeStamp);
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);//Environment.DIRECTORY_PICTURES
        if (!storageDir.exists()) {
            storageDir.mkdir();
        }
        File tempFile = new File(storageDir, imageFileName);
        if (!Environment.MEDIA_MOUNTED.equals(EnvironmentCompat.getStorageState(tempFile))) {
            return null;
        }
        return tempFile;
    }

}
