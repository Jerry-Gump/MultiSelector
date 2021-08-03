package com.donkingliang.imageselector.entry;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

/**
 *图片实体类
 */
public class FileData implements Parcelable {

    private String path;
    private long time;
    private String name;
    private String mimeType;
    private Uri uri;
    private long duration;
    private long size;

    public FileData(String path, long time, String name, String mimeType, Uri uri, long size) {
        this.path = path;
        this.time = time;
        this.name = name;
        this.mimeType = mimeType;
        this.uri = uri;
        this.size = size;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMimeType() {
        return mimeType;
    }

    public void setMimeType(String mimeType) {
        this.mimeType = mimeType;
    }

    public boolean isGif(){
        return "image/gif".equals(mimeType);
    }

    public long getDuration(){return duration;}

    public void setDuration(long duration) {
        this.duration = duration;
    }
    public long getSize(){return size;}
    public void setSize(long size){this.size=size;}

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.path);
        dest.writeLong(this.time);
        dest.writeString(this.name);
        dest.writeString(this.mimeType);
        dest.writeParcelable(this.uri, flags);
        dest.writeLong(this.duration);
        dest.writeLong(this.size);
    }

    protected FileData(Parcel in) {
        this.path = in.readString();
        this.time = in.readLong();
        this.name = in.readString();
        this.mimeType = in.readString();
        this.uri = in.readParcelable(Uri.class.getClassLoader());
        this.duration = in.readLong();
        this.size = in.readLong();
    }

    public static final Creator<FileData> CREATOR = new Creator<FileData>() {
        @Override
        public FileData createFromParcel(Parcel source) {
            return new FileData(source);
        }

        @Override
        public FileData[] newArray(int size) {
            return new FileData[size];
        }
    };
}
