package com.donkingliang.imageselector.adapter;

import android.content.Context;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.donkingliang.imageselector.R;
import com.donkingliang.imageselector.entry.FileData;
import com.donkingliang.imageselector.utils.VersionUtils;

import java.util.ArrayList;

public class SelectorBaseAdapter<VH extends SelectorBaseAdapter.SelectorViewHolder> extends RecyclerView.Adapter<VH>{

    protected Context mContext;
    protected ArrayList<FileData> mFileData;
    protected LayoutInflater mInflater;

    //保存选中的图片
    protected ArrayList<FileData> mSelectFileData = new ArrayList<>();
    protected OnFileSelectListener mSelectListener;
    protected OnItemClickListener mItemClickListener;
    protected int mMaxCount;
    protected boolean isSingle;
    //protected boolean isViewImage;

    protected static final int TYPE_CAMERA = 1;
    protected static final int TYPE_IMAGE = 2;
    protected static final int TYPE_VIDEO = 3;
    protected static final int TYPE_AUDIO = 4;

    protected boolean useCamera;

    protected boolean isAndroidQ = VersionUtils.isAndroidQ();

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {

    }

    @Override
    public int getItemCount() {return useCamera ? getFileCount() + 1 : getFileCount();
    }

    protected int getFileCount() {
        return mFileData == null ? 0 : mFileData.size();
    }

    public ArrayList<FileData> getData() {
        return mFileData;
    }

    public void refresh(ArrayList<FileData> data, boolean useCamera) {
        mFileData = data;
        this.useCamera = useCamera;
        notifyDataSetChanged();
    }

    public void refresh(ArrayList<FileData> data){
        refresh(data, false);
    }

    protected FileData getFile(int position) {
        return mFileData.get(useCamera ? position - 1 : position);
    }

    public FileData getFirstFile(int firstVisibleItem) {
        if (mFileData != null && !mFileData.isEmpty()) {
            if (useCamera) {
                return mFileData.get(firstVisibleItem > 0 ? firstVisibleItem - 1 : 0);
            } else {
                return mFileData.get(firstVisibleItem < 0 ? 0 : firstVisibleItem);
            }
        }
        return null;
    }

    /**
     * 设置图片选中和未选中的效果
     */
    protected void setItemSelect(VH holder, boolean isSelect) {
        if (isSelect) {
            holder.ivSelectIcon.setImageResource(R.drawable.icon_image_select);
            holder.ivMasking.setAlpha(0.5f);
        } else {
            holder.ivSelectIcon.setImageResource(R.drawable.icon_image_un_select);
            holder.ivMasking.setAlpha(0.2f);
        }
    }

    protected void clearFileSelect() {
        if (mFileData != null && mSelectFileData.size() == 1) {
            int index = mFileData.indexOf(mSelectFileData.get(0));
            mSelectFileData.clear();
            if (index != -1) {
                notifyItemChanged(useCamera ? index + 1 : index);
            }
        }
    }

    public void setSelectedFiles(ArrayList<String> selected) {
        if (mFileData != null && selected != null) {
            for (String path : selected) {
                if (isFull()) {
                    return;
                }
                for (FileData fileData : mFileData) {
                    if (path.equals(fileData.getPath())) {
                        if (!mSelectFileData.contains(fileData)) {
                            mSelectFileData.add(fileData);
                        }
                        break;
                    }
                }
            }
            notifyDataSetChanged();
        }
    }


    private boolean isFull() {
        if (isSingle && mSelectFileData.size() == 1) {
            return true;
        } else if (mMaxCount > 0 && mSelectFileData.size() == mMaxCount) {
            return true;
        } else {
            return false;
        }
    }

    protected void checkedFile(VH holder, FileData fileData) {
        if (mSelectFileData.contains(fileData)) {
            //如果图片已经选中，就取消选中
            unSelectFile(fileData);
            setItemSelect(holder, false);
        } else if (isSingle) {
            //如果是单选，就先清空已经选中的图片，再选中当前图片
            clearFileSelect();
            selectFile(fileData);
            setItemSelect(holder, true);
        } else if (mMaxCount <= 0 || mSelectFileData.size() < mMaxCount) {
            //如果不限制图片的选中数量，或者图片的选中数量
            // 还没有达到最大限制，就直接选中当前图片。
            selectFile(fileData);
            setItemSelect(holder, true);
        }
    }

    /**
     * 选中图片
     *
     * @param fileData
     */
    protected void selectFile(FileData fileData) {
        mSelectFileData.add(fileData);
        if (mSelectListener != null) {
            mSelectListener.OnFileSelect(fileData, true, mSelectFileData.size());
        }
    }

    /**
     * 取消选中图片
     *
     * @param fileData
     */
    protected void unSelectFile(FileData fileData) {
        mSelectFileData.remove(fileData);
        if (mSelectListener != null) {
            mSelectListener.OnFileSelect(fileData, false, mSelectFileData.size());
        }
    }

    public ArrayList<FileData> getSelectFiles() {
        return mSelectFileData;
    }

    public void setOnFileSelectListener(OnFileSelectListener listener) {
        this.mSelectListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mItemClickListener = listener;
    }

    public interface OnFileSelectListener {
        void OnFileSelect(FileData fileData, boolean isSelect, int selectCount);
    }

    public interface OnItemClickListener {
        void OnItemClick(FileData fileData, int position);

        void OnCameraClick();
    }

    public static class SelectorViewHolder extends RecyclerView.ViewHolder{

        ImageView ivSelectIcon;
        ImageView ivMasking;

        public SelectorViewHolder(@NonNull View itemView) {
            super(itemView);

            ivSelectIcon = itemView.findViewById(R.id.iv_select);
            ivMasking = itemView.findViewById(R.id.iv_masking);
        }
    }
    /*
    * 工具方法
    * */
    public static String getDurationString(long duration) {
//        long days = duration / (1000 * 60 * 60 * 24);
        long hours = (duration % (1000 * 60 * 60 * 24)) / (1000 * 60 * 60);
        long minutes = (duration % (1000 * 60 * 60)) / (1000 * 60);
        long seconds = (duration % (1000 * 60)) / 1000;

        String hourStr = (hours < 10) ? "0" + hours : hours + "";
        String minuteStr = (minutes < 10) ? "0" + minutes : minutes + "";
        String secondStr = (seconds < 10) ? "0" + seconds : seconds + "";

        if (hours != 0) {
            return hourStr + ":" + minuteStr + ":" + secondStr;
        } else {
            return minuteStr + ":" + secondStr;
        }
    }

    public static int getScreenWidth(Context ctx) {
        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        return dm.widthPixels;
    }

    public static int getScreenHeight(Context ctx) {
        WindowManager wm = (WindowManager) ctx.getSystemService(Context.WINDOW_SERVICE);
        DisplayMetrics dm = new DisplayMetrics();
        wm.getDefaultDisplay().getMetrics(dm);
        return dm.heightPixels;
    }

    public static int dip2px(Context context, float dpValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dpValue * scale + 0.5f);
    }

    public static int px2dip(Context context, float pxValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (pxValue / scale + 0.5f);
    }
}
