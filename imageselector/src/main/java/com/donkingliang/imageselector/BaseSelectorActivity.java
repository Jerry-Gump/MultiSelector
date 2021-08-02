package com.donkingliang.imageselector;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.donkingliang.imageselector.adapter.FolderAdapter;
import com.donkingliang.imageselector.adapter.SelectorBaseAdapter;
import com.donkingliang.imageselector.entry.FileData;
import com.donkingliang.imageselector.entry.Folder;
import com.donkingliang.imageselector.entry.RequestConfig;
import com.donkingliang.imageselector.utils.DateUtils;
import com.donkingliang.imageselector.utils.MultiSelector;
import com.donkingliang.imageselector.utils.VersionUtils;

import java.util.ArrayList;

public abstract class BaseSelectorActivity extends AppCompatActivity {
    protected TextView tvTime;
    protected TextView tvFolderName;
    protected TextView tvConfirm;
    protected TextView tvPreview;
    protected FrameLayout btnConfirm;
    //protected FrameLayout btnPreview;
    protected RecyclerView rvImage;
    protected RecyclerView rvFolder;
    protected View masking;

    protected SelectorBaseAdapter mAdapter;
    protected GridLayoutManager mLayoutManager;

    protected ArrayList<Folder> mFolders;
    protected Folder mFolder;
    protected boolean applyLoadFile = false;
    protected boolean applyCamera = false;
    protected static final int PERMISSION_WRITE_EXTERNAL_REQUEST_CODE = 0x00000011;
    protected static final int PERMISSION_CAMERA_REQUEST_CODE = 0x00000012;

    protected boolean isOpenFolder;
    protected boolean isShowTime;
    protected boolean isInitFolder;
    protected boolean isSingle;
    protected boolean canPreview = true;
    protected int mMaxCount;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        RequestConfig config = intent.getParcelableExtra(MultiSelector.KEY_CONFIG);
        mMaxCount = config.maxSelectCount;
        isSingle = config.isSingle;
        canPreview = config.canPreview;
        mSelectedFiles = config.selected;
    }

    /**
     * 横竖屏切换处理
     *
     * @param newConfig
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (mLayoutManager != null && mAdapter != null) {
            //切换为竖屏
            if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT) {
                mLayoutManager.setSpanCount(3);
            }
            //切换为横屏
            else if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                mLayoutManager.setSpanCount(5);
            }
            mAdapter.notifyDataSetChanged();
        }
    }

    /**
     * 发生没有权限等异常时，显示一个提示dialog.
     */
    protected void showExceptionDialog(final boolean applyLoad, String message) {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.selector_hint)
                .setMessage(message)
                .setNegativeButton(R.string.selector_cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        finish();
                    }
                }).setPositiveButton(R.string.selector_confirm, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
                startAppSettings();
                if (applyLoad) {
                    applyLoadFile = true;
                } else {
                    applyCamera = true;
                }
            }
        }).show();
    }

    /**
     * 检查权限并加载SD卡里的图片。
     */
    protected void checkPermissionAndLoadFiles() {
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
//            Toast.makeText(this, "没有图片", Toast.LENGTH_LONG).show();
            return;
        }
        int hasWriteExternalPermission = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteExternalPermission == PackageManager.PERMISSION_GRANTED) {
            //有权限，加载图片。
            loadFileForSDCard();
        } else {
            //没有权限，申请权限。
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_WRITE_EXTERNAL_REQUEST_CODE);
        }
    }

    /**
     * 从SDCard加载图片。
     */
    protected abstract void loadFileForSDCard();

    protected Handler mHideHandler = new Handler();
    protected Runnable mHide = new Runnable() {
        @Override
        public void run() {
            hideTime();
        }
    };

    protected void initView() {
        rvImage = findViewById(R.id.rv_image);
        rvFolder = findViewById(R.id.rv_folder);
        tvConfirm = findViewById(R.id.tv_confirm);
        tvPreview = findViewById(R.id.tv_preview);
        btnConfirm = findViewById(R.id.btn_confirm);
        tvFolderName = findViewById(R.id.tv_folder_name);
        tvTime = findViewById(R.id.tv_time);
        masking = findViewById(R.id.masking);
    }

    protected void initListener() {
        findViewById(R.id.btn_back).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

        btnConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                confirm();
            }
        });

        findViewById(R.id.btn_folder).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isInitFolder) {
                    if (isOpenFolder) {
                        closeFolder();
                    } else {
                        openFolder();
                    }
                }
            }
        });

        masking.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                closeFolder();
            }
        });

        rvImage.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                changeTime();
            }

            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                changeTime();
            }
        });
    }

    /**
     * 修改状态栏颜色
     */
    protected void setStatusBarColor() {
        if (VersionUtils.isAndroidL()) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(Color.parseColor("#373c3d"));
        }
    }

    //用于接收从外面传进来的已选择的图片列表。当用户原来已经有选择过图片，现在重新打开选择器，允许用
    // 户把先前选过的图片传进来，并把这些图片默认为选中状态。
    protected ArrayList<String> mSelectedFiles;

    /**
     * 设置选中的文件夹，同时刷新图片列表
     *
     * @param folder
     */
    protected void setFolder(Folder folder) {
        if (folder != null && mAdapter != null && !folder.equals(mFolder)) {
            mFolder = folder;
            tvFolderName.setText(folder.getName());
            rvImage.scrollToPosition(0);
            mAdapter.refresh(folder.getFiles(), folder.isUseCamera());
        }
    }

    /**
     * 隐藏时间条
     */
    protected void hideTime() {
        if (isShowTime) {
            ObjectAnimator.ofFloat(tvTime, "alpha", 1, 0).setDuration(300).start();
            isShowTime = false;
        }
    }

    /**
     * 显示时间条
     */
    protected void showTime() {
        if (!isShowTime) {
            ObjectAnimator.ofFloat(tvTime, "alpha", 0, 1).setDuration(300).start();
            isShowTime = true;
        }
    }


    /**
     * 初始化图片文件夹列表
     */
    protected void initFolderList(int numRes) {
        if (mFolders != null && !mFolders.isEmpty()) {
            isInitFolder = true;
            rvFolder.setLayoutManager(new LinearLayoutManager(this));
            FolderAdapter adapter = new FolderAdapter(this, mFolders, numRes);
            adapter.setOnFolderSelectListener(new FolderAdapter.OnFolderSelectListener() {
                @Override
                public void OnFolderSelect(Folder folder) {
                    setFolder(folder);
                    closeFolder();
                }
            });
            rvFolder.setAdapter(adapter);
        }
    }

    /**
     * 刚开始的时候文件夹列表默认是隐藏的
     */
    protected void hideFolderList() {
        rvFolder.post(new Runnable() {
            @Override
            public void run() {
                rvFolder.setTranslationY(rvFolder.getHeight());
                rvFolder.setVisibility(View.GONE);
                rvFolder.setBackgroundColor(Color.WHITE);
            }
        });
    }

    protected void setSelectFileCount(int count) {
        if (count == 0) {
            btnConfirm.setEnabled(false);
            //btnPreview.setEnabled(false);
            tvConfirm.setText(R.string.selector_send);
            tvPreview.setText(R.string.selector_preview);
        } else {
            btnConfirm.setEnabled(true);
            //btnPreview.setEnabled(true);
            tvPreview.setText(getString(R.string.selector_preview) + "(" + count + ")");
            if (isSingle) {
                tvConfirm.setText(R.string.selector_send);
            } else if (mMaxCount > 0) {
                tvConfirm.setText(getString(R.string.selector_send) + "(" + count + "/" + mMaxCount + ")");
            } else {
                tvConfirm.setText(getString(R.string.selector_send) + "(" + count + ")");
            }
        }
    }

    /**
     * 弹出文件夹列表
     */
    protected void openFolder() {
        if (!isOpenFolder) {
            masking.setVisibility(View.VISIBLE);
            ObjectAnimator animator = ObjectAnimator.ofFloat(rvFolder, "translationY",
                    rvFolder.getHeight(), 0).setDuration(300);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationStart(Animator animation) {
                    super.onAnimationStart(animation);
                    rvFolder.setVisibility(View.VISIBLE);
                }
            });
            animator.start();
            isOpenFolder = true;
        }
    }

    /**
     * 收起文件夹列表
     */
    protected void closeFolder() {
        if (isOpenFolder) {
            masking.setVisibility(View.GONE);
            ObjectAnimator animator = ObjectAnimator.ofFloat(rvFolder, "translationY",
                    0, rvFolder.getHeight()).setDuration(300);
            animator.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    super.onAnimationEnd(animation);
                    rvFolder.setVisibility(View.GONE);
                }
            });
            animator.start();
            isOpenFolder = false;
        }
    }


    /**
     * 改变时间条显示的时间（显示图片列表中的第一个可见图片的时间）
     */
    protected void changeTime() {
        int firstVisibleItem = getFirstVisibleItem();
        FileData fileData = mAdapter.getFirstFile(firstVisibleItem);
        if (fileData != null) {
            String time = DateUtils.getImageTime(this, fileData.getTime());
            tvTime.setText(time);
            showTime();
            mHideHandler.removeCallbacks(mHide);
            mHideHandler.postDelayed(mHide, 1500);
        }
    }

    protected int getFirstVisibleItem() {
        return mLayoutManager.findFirstVisibleItemPosition();
    }

    protected void confirm() {
        if (mAdapter == null) {
            return;
        }
        //因为图片的实体类是Image，而我们返回的是String数组，所以要进行转换。
        ArrayList<FileData> selectFileData = mAdapter.getSelectFiles();
        ArrayList<String> files = new ArrayList<>();
        for (FileData fileData : selectFileData) {
            files.add(fileData.getPath());
        }
        saveFileAndFinish(files, false);
    }

    protected void saveFileAndFinish(final ArrayList<String> files, final boolean isFromCamera) {

        //点击确定，把选中的图片通过Intent传给上一个Activity。
        setResult(files, isFromCamera);
        finish();
    }

    protected void setResult(ArrayList<String> files, boolean isFromCamera) {
        Intent intent = new Intent();
        intent.putStringArrayListExtra(MultiSelector.SELECT_RESULT, files);
        intent.putExtra(MultiSelector.IS_FROM_CAMERA, isFromCamera);
        setResult(RESULT_OK, intent);
    }
    /**
     * 启动应用的设置
     */
    protected void startAppSettings() {
        Intent intent = new Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && event.getAction() == KeyEvent.ACTION_DOWN && isOpenFolder) {
            closeFolder();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
