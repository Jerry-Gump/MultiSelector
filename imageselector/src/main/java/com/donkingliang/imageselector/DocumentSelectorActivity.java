package com.donkingliang.imageselector;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.SimpleItemAnimator;

import com.donkingliang.imageselector.adapter.DocumentAdapter;
import com.donkingliang.imageselector.adapter.SelectorBaseAdapter;
import com.donkingliang.imageselector.entry.FileData;
import com.donkingliang.imageselector.entry.FilePreviewListener;
import com.donkingliang.imageselector.entry.Folder;
import com.donkingliang.imageselector.entry.RequestConfig;
import com.donkingliang.imageselector.model.BaseModel;
import com.donkingliang.imageselector.model.DocumentModel;
import com.donkingliang.imageselector.utils.MultiSelector;

import java.util.ArrayList;

public class DocumentSelectorActivity extends BaseSelectorActivity {

    private String fileType = DocumentModel.FILE_TYPE_DOCUMENT;
    private String suffix = "";

    /**
     * 启动图片选择器
     *
     * @param activity
     * @param requestCode
     * @param config
     */
    public static void openActivity(Activity activity, int requestCode, RequestConfig config) {
        Intent intent = new Intent(activity, DocumentSelectorActivity.class);
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
        Intent intent = new Intent(fragment.getActivity(), DocumentSelectorActivity.class);
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
        Intent intent = new Intent(fragment.getActivity(), DocumentSelectorActivity.class);
        intent.putExtra(MultiSelector.KEY_CONFIG, config);
        fragment.startActivityForResult(intent, requestCode);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        RequestConfig config = intent.getParcelableExtra(MultiSelector.KEY_CONFIG);
        fileType = config.fileType;
        suffix = config.suffix;

        setContentView(R.layout.activity_document_select);
        setStatusBarColor();
        initView();
        initListener();
        initDocumentList();
        checkPermissionAndLoadFiles();
        hideFolderList();
        setSelectFileCount(0);
    }

    @Override
    protected void initView(){
        super.initView();
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
                    if(mPreviewListener != null){
                        mPreviewListener.onPreview(DocumentSelectorActivity.this, fileData.get(0));
                    }else {
                        openDocumentFile(fileData.get(0));
                    }
                }
            }
        });
    }

    private void openDocumentFile(FileData fileData){
        Intent intent=new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri= fileData.getUri();
        intent.setDataAndType(uri, fileData.getMimeType());
        startActivity(intent);
        /*
        Intent intent=new Intent(this, FilePreviewActivity.class);
        intent.putExtra(FilePreviewActivity.FILE_DATA, fileData);
        startActivity(intent);*/
    }

    /**
     * 初始化图片列表
     */
    private void initDocumentList() {
        // 判断屏幕方向
        Configuration configuration = getResources().getConfiguration();
        /*
        if (configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            mLayoutManager = new GridLayoutManager(this, 3);
        } else {
            mLayoutManager = new GridLayoutManager(this, 5);
        }*/
        mLayoutManager = new GridLayoutManager(this, 1);

        rvImage.setLayoutManager(mLayoutManager);
        mAdapter = new DocumentAdapter(this, mMaxCount, isSingle, canPreview);
        rvImage.setAdapter(mAdapter);
        ((SimpleItemAnimator) rvImage.getItemAnimator()).setSupportsChangeAnimations(false);
        if (mFolders != null && !mFolders.isEmpty()) {
            setFolder(mFolders.get(0));
        }
        mAdapter.setOnFileSelectListener(new SelectorBaseAdapter.OnFileSelectListener() {
            @Override
            public void OnFileSelect(FileData fileData, boolean isSelect, int selectCount) {
                setSelectFileCount(selectCount);
            }
        });
        mAdapter.setOnItemClickListener(new SelectorBaseAdapter.OnItemClickListener() {
            @Override
            public void OnItemClick(FileData fileData, int position) {
                // 视频暂时不考虑预览，而应该尝试启用第三方播放应用，因为这个播放多种格式视频需要用到ffmpeg，规模太大了，不适应这个轻量化的思路
                //toPreviewActivity(mAdapter.getData(), position);
                if(canPreview) {
                    if (mPreviewListener != null) {
                        mPreviewListener.onPreview(DocumentSelectorActivity.this, fileData);
                    }else{
                        openDocumentFile(fileData);
                    }
                }
            }

            @Override
            public void OnCameraClick() {

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
                showExceptionDialog(true, getString(R.string.selector_audio_permission_hint));
            }
        }
    }

    /**
     * 从SDCard加载图片。
     */
    protected void loadFileForSDCard() {
        DocumentModel.loadDocumentForSDCard(this,fileType, suffix, new BaseModel.DataCallback() {
            @Override
            public void onSuccess(ArrayList<Folder> folders) {
                mFolders = folders;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (mFolders != null && !mFolders.isEmpty()) {
                            initFolderList(R.string.selector_file_num);
                            mFolders.get(0).setUseCamera(false);
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
}
