package com.donkingliang.imageselector.model;

import android.Manifest;
import android.content.ContentResolver;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.webkit.MimeTypeMap;

import androidx.core.content.ContextCompat;

import com.donkingliang.imageselector.R;
import com.donkingliang.imageselector.entry.FileData;
import com.donkingliang.imageselector.entry.Folder;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class DocumentModel {
    public final static String FILE_TYPE_ALL = "all";
    public final static String FILE_TYPE_DOCUMENT = "document";
    public final static String FILE_TYPE_CUSTOM = "custom";
    public final static String FILE_TYPE_NONE = "none";
    /**
     * 缓存图片
     */
    private static ArrayList<Folder> cacheList = null;
    private static boolean isNeedCache = false;
    private static DocumentContentObserver observer;

    /**
     * 预加载图片
     *
     * @param context
     */
    public static void preloadAndRegisterContentObserver(final Context context, final String fileType, final String suffix) {
        isNeedCache = true;
        if (observer == null) {
            observer = new DocumentContentObserver(context.getApplicationContext(), fileType, suffix);
            context.getApplicationContext().getContentResolver().registerContentObserver(
                    MediaStore.Files.getContentUri("external"), true, observer);
        }
        preload(context,fileType,suffix);
    }

    private static void preload(final Context context, final String fileType, final String suffix) {
        int hasWriteExternalPermission = ContextCompat.checkSelfPermission(context,
                Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (hasWriteExternalPermission == PackageManager.PERMISSION_GRANTED) {
            //有权限，加载图片。
            loadDocumentForSDCard(context, true,fileType, suffix, null);
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
                synchronized (DocumentModel.class) {
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
    public static void loadDocumentForSDCard(final Context context,
                                             final String fileType,
                                             final String suffix,
                                             final BaseModel.DataCallback callback) {
        loadDocumentForSDCard(context, false,fileType, suffix, callback);
    }

    /**
     * 从SDCard加载图片
     *
     * @param context
     * @param isPreload 是否是预加载
     * @param callback
     */
    private static void loadDocumentForSDCard(final Context context,
                                              final boolean isPreload,
                                              final String fileType,
                                              final String suffix,
                                              final BaseModel.DataCallback callback) {
        //由于扫描图片是耗时的操作，所以要在子线程处理。
        new Thread(new Runnable() {
            @Override
            public void run() {
                synchronized (DocumentModel.class) {
                    ArrayList<Folder> folders = null;
                    if (cacheList == null || isPreload) {
                        ArrayList<FileData> fileDataList = loadDocument(context, fileType, suffix);
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
                        folders = BaseModel.splitFolder(context,context.getString(R.string.selector_all_document), fileDatas);
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
    private static synchronized ArrayList<FileData> loadDocument(Context context, final String fileType, final String suffix) {
        //扫描图片
        Uri mSourceUri = MediaStore.Files.getContentUri("external");
        ContentResolver mContentResolver = context.getContentResolver();

        String[] projection = new String[]{
                MediaStore.Files.FileColumns.DATA,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.SIZE};
        String[] args = null;
        String selection = MediaStore.MediaColumns.SIZE + ">0";
        if(fileType == FILE_TYPE_DOCUMENT){
            if (Build.VERSION.SDK_INT >= 30) {
                selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_DOCUMENT;
            }else{
                selection = "";
                String[] ss = new String[]{"doc","docx","xls","xlsx","ppt","pptx","pdf","txt"};
                ArrayList<String> mimeArray = new ArrayList<>();
                for (String s:
                        ss) {
                    if(!TextUtils.isEmpty(s)){
                        if(MimeTypeMap.getSingleton().hasExtension(s)) {
                            mimeArray.add(MimeTypeMap.getSingleton().getMimeTypeFromExtension(s));
                        }
                    }
                }
                args = mimeArray.toArray(new String[mimeArray.size()]);
                for(int i = 0; i<args.length; i++){
                    if(i>0){
                        selection += " or ";
                    }
                    selection += MediaStore.Files.FileColumns.MIME_TYPE+"=?";
                }
            }
        }else if(fileType == FILE_TYPE_CUSTOM){
            selection = "";
            String[] ss = suffix.split(",");
            ArrayList<String> mimeArray = new ArrayList<>();
            for (String s:
                 ss) {
                if(!TextUtils.isEmpty(s)){
                    if(MimeTypeMap.getSingleton().hasExtension(s)) {
                        mimeArray.add(MimeTypeMap.getSingleton().getMimeTypeFromExtension(s));
                    }
                }
            }
            args = (String[]) mimeArray.toArray(new String[mimeArray.size()]);
            for(int i = 0; i<args.length; i++){
                if(i>0){
                    selection += " or ";
                }
                selection += MediaStore.Files.FileColumns.MIME_TYPE+"=?";
            }
        }else if(fileType == FILE_TYPE_NONE){
            selection = MediaStore.Files.FileColumns.MEDIA_TYPE + "=" + MediaStore.Files.FileColumns.MEDIA_TYPE_NONE;
        }
        Cursor mCursor = mContentResolver.query(mSourceUri, projection,
                selection,
                args,
                MediaStore.Files.FileColumns.DATE_ADDED + " DESC");

        ArrayList<FileData> fileData = new ArrayList<>();

        //读取扫描到的图片
        if (mCursor != null) {
            while (mCursor.moveToNext()) {
                // 获取图片的路径
                long id = mCursor.getLong(mCursor.getColumnIndex(MediaStore.Files.FileColumns._ID));
                String path = mCursor.getString(
                        mCursor.getColumnIndex(MediaStore.Files.FileColumns.DATA));
                //获取图片名称
                String name = mCursor.getString(
                        mCursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME));
                if(TextUtils.isEmpty(name)){
                    name = path.substring(path.lastIndexOf(File.separator) + 1);
                }
                //获取图片时间
                long time = mCursor.getLong(
                        mCursor.getColumnIndex(MediaStore.Files.FileColumns.DATE_ADDED));

                if (String.valueOf(time).length() < 13) {
                    time *= 1000;
                }

                long size = mCursor.getLong(mCursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE));

                //获取图片类型
                String mimeType = mCursor.getString(
                        mCursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE));

                //获取图片uri
                Uri uri = mSourceUri.buildUpon()
                        .appendPath(String.valueOf(id)).build();

                fileData.add(new FileData(path, time, name, mimeType, uri,size));
            }
            mCursor.close();
        }
        return fileData;
    }

    private static class DocumentContentObserver extends ContentObserver {

        private Context context;
        final private String fileType;
        final private String suffix;

        public DocumentContentObserver(Context appContext, String fileType, String suffix) {
            super(null);
            context = appContext;
            this.fileType = fileType;
            this.suffix = suffix;
        }

        @Override
        public void onChange(boolean selfChange, Uri uri) {
            super.onChange(selfChange, uri);
            preload(context, fileType, suffix);
        }
    }
}
