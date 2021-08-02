package com.donkingliang.imageselector.model;

import android.content.Context;
import android.provider.MediaStore;

import com.donkingliang.imageselector.R;
import com.donkingliang.imageselector.entry.FileData;
import com.donkingliang.imageselector.entry.Folder;
import com.donkingliang.imageselector.utils.StringUtils;
import com.donkingliang.imageselector.utils.UriUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class BaseModel {

    public interface DataCallback {
        void onSuccess(ArrayList<Folder> folders);
    }
    /**
     * 把图片按文件夹拆分，第一个文件夹保存所有的图片
     *
     * @param fileData
     * @return
     */
    public static ArrayList<Folder> splitFolder(Context context, String commonTitle, ArrayList<FileData> fileData) {
        ArrayList<Folder> folders = new ArrayList<>();
        folders.add(new Folder(commonTitle, fileData));

        if (fileData != null && !fileData.isEmpty()) {
            int size = fileData.size();
            for (int i = 0; i < size; i++) {
                String path = fileData.get(i).getPath();
                String name = getFolderName(path);
                if (StringUtils.isNotEmptyString(name)) {
                    Folder folder = getFolder(name, folders);
                    folder.addFile(fileData.get(i));
                }
            }
        }
        return folders;
    }

    /**
     * Java文件操作 获取文件扩展名
     */
    public static String getExtensionName(String filename) {
        if (filename != null && filename.length() > 0) {
            int dot = filename.lastIndexOf('.');
            if (dot > -1 && dot < filename.length() - 1) {
                return filename.substring(dot + 1);
            }
        }
        return "";
    }

    /**
     * 根据图片路径，获取图片文件夹名称
     *
     * @param path
     * @return
     */
    public static String getFolderName(String path) {
        if (StringUtils.isNotEmptyString(path)) {
            String[] strings = path.split(File.separator);
            if (strings.length >= 2) {
                return strings[strings.length - 2];
            }
        }
        return "";
    }

    public static Folder getFolder(String name, List<Folder> folders) {
        if (!folders.isEmpty()) {
            int size = folders.size();
            for (int i = 0; i < size; i++) {
                Folder folder = folders.get(i);
                if (name.equals(folder.getName())) {
                    return folder;
                }
            }
        }
        Folder newFolder = new Folder(name);
        folders.add(newFolder);
        return newFolder;
    }
    /**
     * 检查图片是否存在。ContentResolver查询处理的数据有可能文件路径并不存在。
     *
     * @param filePath
     * @return
     */
    public static boolean checkFileExists(String filePath) {
        return new File(filePath).exists();
    }

    public static String getPathForAndroidQ(Context context, long id) {
        return UriUtils.getPathForUri(context, MediaStore.Video.Media.EXTERNAL_CONTENT_URI.buildUpon()
                .appendPath(String.valueOf(id)).build());
    }
}
