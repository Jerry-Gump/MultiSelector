package com.donkingliang.imageselector.model;

import static android.provider.MediaStore.MediaColumns.DURATION;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.core.content.ContextCompat;

import com.donkingliang.imageselector.R;
import com.donkingliang.imageselector.entry.FileData;
import com.donkingliang.imageselector.entry.Folder;
import com.donkingliang.imageselector.utils.ImageUtil;
import com.donkingliang.imageselector.utils.StringUtils;
import com.donkingliang.imageselector.utils.UriUtils;
import com.donkingliang.imageselector.utils.VersionUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class VideoModel {

    /**
     * 缓存图片
     */
    private static ArrayList<Folder> cacheList = null;
    private static boolean isNeedCache = false;
    private static VideoModel.VideoContentObserver observer;

    /**
     * 预加载图片
     *
     * @param context
     */
    public static void preloadAndRegisterContentObserver(final Context context) {
        isNeedCache = true;
        if (observer == null) {
            observer = new VideoModel.VideoContentObserver(context.getApplicationContext());
            context.getApplicationContext().getContentResolver().registerContentObserver(
                    MediaStore.Video.Media.EXTERNAL_CONTENT_URI, true, observer);
        }
        preload(context);
    }

    private static void preload(final Context context) {
        int hasWriteExternalPermission = ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteExternalPermission == PackageManager.PERMISSION_GRANTED) {
            //有权限，加载图片。
            loadVideoForSDCard(context, true, null);
        }
    }

    /**
     * 清空缓存
     */
    public static void clearCache(Context context) {
        isNeedCache = false;
        if (observer != null) {
            context.getApplicationContext().getContentResolver().unregisterContentObserver(observer);
            observer = null;
        }
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (VideoModel.class) {
                    if (cacheList != null) {
                        cacheList.clear();
                        cacheList = null;
                    }
                }
            }
        }).start();
    }
    /**
     * 获取缓存图片的文件夹
     *
     * @param context
     * @return
     */
    public static String getVideoCacheDir(Context context) {
        File file = null;
        if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState())
                || !Environment.isExternalStorageRemovable()) {
            if (VersionUtils.isAndroidQ()) {
                file = context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
            } else {
                file = context.getExternalCacheDir();
            }
        }

        if (file == null) {
            file = context.getCacheDir();
        }
        return file.getPath() + File.separator + "image_select";
    }

    /**
     * 从SDCard加载图片
     *
     * @param context
     * @param callback
     */
    public static void loadVideoForSDCard(final Context context, final BaseModel.DataCallback callback) {
        loadVideoForSDCard(context, false, callback);
    }

    /**
     * 从SDCard加载图片
     *
     * @param context
     * @param isPreload 是否是预加载
     * @param callback
     */
    private static void loadVideoForSDCard(final Context context, final boolean isPreload, final BaseModel.DataCallback callback) {
        //由于扫描图片是耗时的操作，所以要在子线程处理。
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (VideoModel.class) {
                    //String imageCacheDir = getVideoCacheDir(context);
                    ArrayList<Folder> folders = null;
                    if (cacheList == null || isPreload) {
                        ArrayList<FileData> fileDataList = loadVideo(context);
                        Collections.sort(fileDataList, new Comparator<FileData>() {
                            @Override
                            public int compare(FileData fileData, FileData t1) {
                                if (fileData.getTime() > t1.getTime()) {
                                    return 1;
                                } else if (fileData.getTime() < t1.getTime()) {
                                    return -1;
                                } else {
                                    return 0;
                                }
                            }
                        });
                        ArrayList<FileData> fileDatas = new ArrayList<>();

                        for (FileData fileData : fileDataList) {
                            // 过滤不存在或未下载完成的图片
                            boolean exists = !"downloading".equals(BaseModel.getExtensionName(fileData.getPath())) && BaseModel.checkFileExists(fileData.getPath());
                            if(exists){
                                fileDatas.add(fileData);
                            }
                            /*
                            //过滤剪切保存的图片；
                            boolean isCutImage = ImageUtil.isCutImage(imageCacheDir, fileData.getPath());
                            if (!isCutImage && exists) {
                                fileDatas.add(fileData);
                            }*/
                        }
                        Collections.reverse(fileDatas);
                        folders = BaseModel.splitFolder(context,context.getString(R.string.selector_all_video), fileDatas);
                        if (isNeedCache) {
                            cacheList = folders;
                        }
                    } else {
                        folders = cacheList;
                    }

                    if (callback != null) {
                        callback.onSuccess(folders);
                    }
                }
            }
        }).start();
    }

    /**
     * 从SDCard加载图片
     *
     * @param context
     * @return
     */
    private static synchronized ArrayList<FileData> loadVideo(Context context) {
        //扫描图片
        Uri mImageUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        ContentResolver mContentResolver = context.getContentResolver();

        Cursor mCursor = mContentResolver.query(mImageUri, new String[]{
                        MediaStore.Video.Media.DATA,
                        MediaStore.Video.Media.DISPLAY_NAME,
                        MediaStore.Video.Media.DATE_ADDED,
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.MIME_TYPE,
                        MediaStore.Video.Media.SIZE,
                        MediaStore.Video.Media.DURATION},
                MediaStore.MediaColumns.SIZE + ">0",
                null,
                MediaStore.Video.Media.DATE_ADDED + " DESC");

        ArrayList<FileData> fileData = new ArrayList<>();

        //读取扫描到的图片
        if (mCursor != null) {
            while (mCursor.moveToNext()) {
                // 获取图片的路径
                long id = mCursor.getLong(mCursor.getColumnIndex(MediaStore.Video.Media._ID));
                String path = mCursor.getString(
                        mCursor.getColumnIndex(MediaStore.Video.Media.DATA));
                //获取图片名称
                String name = mCursor.getString(
                        mCursor.getColumnIndex(MediaStore.Video.Media.DISPLAY_NAME));
                //获取图片时间
                long time = mCursor.getLong(
                        mCursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED));

                if (String.valueOf(time).length() < 13) {
                    time *= 1000;
                }

                //获取图片类型
                String mimeType = mCursor.getString(
                        mCursor.getColumnIndex(MediaStore.Video.Media.MIME_TYPE));

                //获取图片uri
                Uri uri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI.buildUpon()
                        .appendPath(String.valueOf(id)).build();

                long duration = mCursor.getLong(mCursor.getColumnIndexOrThrow(DURATION));
                FileData fd = new FileData(path, time, name, mimeType, uri);
                fd.setDuration(duration);

                fileData.add(fd);
            }
            mCursor.close();
        }
        return fileData;
    }

    private static class VideoContentObserver extends ContentObserver {

        private Context context;

        public VideoContentObserver(Context appContext) {
            super(null);
            context = appContext;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            preload(context);
        }
    }
}
