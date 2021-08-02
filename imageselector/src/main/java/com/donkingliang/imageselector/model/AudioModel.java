package com.donkingliang.imageselector.model;

import static android.provider.MediaStore.MediaColumns.DURATION;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.core.content.ContextCompat;

import com.donkingliang.imageselector.R;
import com.donkingliang.imageselector.entry.FileData;
import com.donkingliang.imageselector.entry.Folder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class AudioModel {
    /**
     * 缓存图片
     */
    private static ArrayList<Folder> cacheList = null;
    private static boolean isNeedCache = false;
    private static AudioContentObserver observer;

    /**
     * 预加载图片
     *
     * @param context
     */
    public static void preloadAndRegisterContentObserver(final Context context) {
        isNeedCache = true;
        if (observer == null) {
            observer = new AudioContentObserver(context.getApplicationContext());
            context.getApplicationContext().getContentResolver().registerContentObserver(
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, true, observer);
        }
        preload(context);
    }

    private static void preload(final Context context) {
        int hasWriteExternalPermission = ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteExternalPermission == PackageManager.PERMISSION_GRANTED) {
            //有权限，加载图片。
            loadAudioForSDCard(context, true, null);
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
                synchronized (AudioModel.class) {
                    if (cacheList != null) {
                        cacheList.clear();
                        cacheList = null;
                    }
                }
            }
        }).start();
    }

    /**
     * 从SDCard加载图片
     *
     * @param context
     * @param callback
     */
    public static void loadAudioForSDCard(final Context context, final BaseModel.DataCallback callback) {
        loadAudioForSDCard(context, false, callback);
    }

    /**
     * 从SDCard加载图片
     *
     * @param context
     * @param isPreload 是否是预加载
     * @param callback
     */
    private static void loadAudioForSDCard(final Context context, final boolean isPreload, final BaseModel.DataCallback callback) {
        //由于扫描图片是耗时的操作，所以要在子线程处理。
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (AudioModel.class) {
                    ArrayList<Folder> folders = null;
                    if (cacheList == null || isPreload) {
                        ArrayList<FileData> fileDataList = loadAudio(context);
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
                        }
                        Collections.reverse(fileDatas);
                        folders = BaseModel.splitFolder(context,context.getString(R.string.selector_all_audio), fileDatas);
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
    private static synchronized ArrayList<FileData> loadAudio(Context context) {
        //扫描图片
        Uri mImageUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        ContentResolver mContentResolver = context.getContentResolver();

        Cursor mCursor = mContentResolver.query(mImageUri, new String[]{
                        MediaStore.Audio.Media.DATA,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.DATE_ADDED,
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.MIME_TYPE,
                        MediaStore.Audio.Media.SIZE,
                        MediaStore.Audio.Media.DURATION},
                MediaStore.MediaColumns.SIZE + ">0",
                null,
                MediaStore.Audio.Media.DATE_ADDED + " DESC");

        ArrayList<FileData> fileData = new ArrayList<>();

        //读取扫描到的图片
        if (mCursor != null) {
            while (mCursor.moveToNext()) {
                // 获取图片的路径
                long id = mCursor.getLong(mCursor.getColumnIndex(MediaStore.Audio.Media._ID));
                String path = mCursor.getString(
                        mCursor.getColumnIndex(MediaStore.Audio.Media.DATA));
                //获取图片名称
                String name = mCursor.getString(
                        mCursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                //获取图片时间
                long time = mCursor.getLong(
                        mCursor.getColumnIndex(MediaStore.Audio.Media.DATE_ADDED));

                if (String.valueOf(time).length() < 13) {
                    time *= 1000;
                }

                //获取图片类型
                String mimeType = mCursor.getString(
                        mCursor.getColumnIndex(MediaStore.Audio.Media.MIME_TYPE));

                //获取图片uri
                Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI.buildUpon()
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

    private static class AudioContentObserver extends ContentObserver {

        private Context context;

        public AudioContentObserver(Context appContext) {
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
