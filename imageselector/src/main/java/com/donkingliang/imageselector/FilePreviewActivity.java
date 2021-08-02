package com.donkingliang.imageselector;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.donkingliang.imageselector.adapter.DocumentAdapter;
import com.donkingliang.imageselector.entry.FileData;
import com.tencent.smtt.sdk.TbsReaderView;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;

import es.dmoral.toasty.Toasty;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

public class FilePreviewActivity extends AppCompatActivity implements TbsReaderView.ReaderCallback {
    public static final String FILE_DATA = "file_data";
    private TbsReaderView mTbsReaderView;
    private RelativeLayout rl_tbsView;
    private FileData mFileData;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_preview);
        TextView titleView = findViewById(R.id.tv_toolbar_title);
        ImageButton btnBack = findViewById(R.id.ib_toolbar_back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mFileData = getIntent().getParcelableExtra(FILE_DATA);
        rl_tbsView = findViewById(R.id.container);
        //视频打开方式
        if(DocumentAdapter.isVideo(mFileData.getMimeType())){
            openVideoFile(mFileData);
            finish();
        }else if(DocumentAdapter. isAudio(mFileData.getMimeType())){
            openAudioFile(mFileData);
            finish();
        }
        else if(DocumentAdapter.isImage(mFileData.getMimeType())){
            ArrayList<FileData> datas = new ArrayList<>();
            datas.add(mFileData);
            ImagePreviewActivity.openActivity(this, datas,
                    datas, true, 1, 0);
            finish();
        }
        else{
            //pdf、office文档
            //装载TbsReaderView视图
            mTbsReaderView = new TbsReaderView(this,this);
            rl_tbsView.addView(mTbsReaderView,new RelativeLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));
            displayFile();
        }
    }

    private void openAudioFile(FileData fileData){
        Intent intent=new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri= fileData.getUri();
        intent.setDataAndType(uri, "audio/*");
        startActivity(intent);
    }


    private void openVideoFile(FileData fileData){
        Intent intent=new Intent();
        intent.setAction(Intent.ACTION_VIEW);
        Uri uri= fileData.getUri();
        intent.setDataAndType(uri, "video/*");
        startActivity(intent);
    }

    /**
     * 加载显示文件内容
     */
    private void displayFile() {
        Bundle bundle = new Bundle();
        bundle.putString("filePath", getLocalFile().getPath());
        bundle.putString("tempPath", Environment.getExternalStorageDirectory()
                .getPath());
        boolean result = mTbsReaderView.preOpen(parseFormat(mFileData.getName()), false);
        if (result) {
            mTbsReaderView.openFile(bundle);
        } else {
            Intent openintent = new Intent();
            openintent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            // 设置intent的data和Type属性。
            openintent.setDataAndType(mFileData.getUri(), mFileData.getMimeType());
            // 跳转
            startActivity(openintent);
            finish();
        }
    }

    private String parseFormat(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1);
    }

    private File getLocalFile() {
        return new File(mFileData.getPath());//APP专属文件，随APP删除而删除,getCacheDir()对应的是清除缓存，getFilesDir()对应的是清除数据
    }

    @Override
    public void onCallBackAction(Integer integer, Object o, Object o1) {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(mTbsReaderView != null){
            ViewParent parent = mTbsReaderView.getParent();
            if(parent != null){
                ((ViewGroup)parent).removeView(mTbsReaderView);
            }
            mTbsReaderView.onStop();
        }
    }

    @SuppressLint("Override")
    public void onPointerCaptureChanged(boolean hasCapture) {

    }
}
