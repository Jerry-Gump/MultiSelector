package com.donkingliang.imageselector.entry;


import com.donkingliang.imageselector.utils.StringUtils;

import java.util.ArrayList;

/**
 * 图片文件夹实体类
 */
public class Folder {

    private boolean useCamera; // 是否可以调用相机拍照。只有“全部”文件夹才可以拍照
    private String name;
    private ArrayList<FileData> fileData;

    public Folder(String name) {
        this.name = name;
    }

    public Folder(String name, ArrayList<FileData> fileData) {
        this.name = name;
        this.fileData = fileData;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ArrayList<FileData> getFiles() {
        return fileData;
    }

    public void setFiles(ArrayList<FileData> fileData) {
        this.fileData = fileData;
    }

    public boolean isUseCamera() {
        return useCamera;
    }

    public void setUseCamera(boolean useCamera) {
        this.useCamera = useCamera;
    }

    public void addFile(FileData fileData) {
        if (fileData != null && StringUtils.isNotEmptyString(fileData.getPath())) {
            if (this.fileData == null) {
                this.fileData = new ArrayList<>();
            }
            this.fileData.add(fileData);
        }
    }

    @Override
    public String toString() {
        return "Folder{" +
                "name='" + name + '\'' +
                ", files=" + fileData +
                '}';
    }
}
