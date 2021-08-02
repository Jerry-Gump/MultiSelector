package com.donkingliang.imageselector.adapter;

import android.content.Context;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
import com.donkingliang.imageselector.R;
import com.donkingliang.imageselector.entry.FileData;
import com.donkingliang.imageselector.utils.VersionUtils;

import java.util.ArrayList;

public class ImageAdapter extends RecyclerView.Adapter<ImageAdapter.ViewHolder> {

    private Context mContext;
    private ArrayList<FileData> mFileData;
    private LayoutInflater mInflater;

    //保存选中的图片
    private ArrayList<FileData> mSelectFileData = new ArrayList<>();
    private OnImageSelectListener mSelectListener;
    private OnItemClickListener mItemClickListener;
    private int mMaxCount;
    private boolean isSingle;
    private boolean isViewImage;

    private static final int TYPE_CAMERA = 1;
    private static final int TYPE_IMAGE = 2;

    private boolean useCamera;

    private boolean isAndroidQ = VersionUtils.isAndroidQ();

    /**
     * @param maxCount    图片的最大选择数量，小于等于0时，不限数量，isSingle为false时才有用。
     * @param isSingle    是否单选
     * @param isViewImage 是否点击放大图片查看
     */
    public ImageAdapter(Context context, int maxCount, boolean isSingle, boolean isViewImage) {
        mContext = context;
        this.mInflater = LayoutInflater.from(mContext);
        mMaxCount = maxCount;
        this.isSingle = isSingle;
        this.isViewImage = isViewImage;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_IMAGE) {
            View view = mInflater.inflate(R.layout.adapter_images_item, parent, false);
            return new ViewHolder(view);
        } else {
            View view = mInflater.inflate(R.layout.adapter_camera, parent, false);
            return new ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_IMAGE) {
            final FileData fileData = getImage(position);
            Glide.with(mContext).load(isAndroidQ ? fileData.getUri() : fileData.getPath())
                    .apply(new RequestOptions().diskCacheStrategy(DiskCacheStrategy.NONE))
                    .into(holder.ivImage);

            setItemSelect(holder, mSelectFileData.contains(fileData));

            holder.ivGif.setVisibility(fileData.isGif() ? View.VISIBLE : View.GONE);

            //点击选中/取消选中图片
            holder.ivSelectIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkedImage(holder, fileData);
                }
            });

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (isViewImage) {
                        if (mItemClickListener != null) {
                            int p = holder.getAbsoluteAdapterPosition();// holder.getAdapterPosition();
                            mItemClickListener.OnItemClick(fileData, useCamera ? p - 1 : p);
                        }
                    } else {
                        checkedImage(holder, fileData);
                    }
                }
            });
        } else if (getItemViewType(position) == TYPE_CAMERA) {
            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (mItemClickListener != null) {
                        mItemClickListener.OnCameraClick();
                    }
                }
            });
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (useCamera && position == 0) {
            return TYPE_CAMERA;
        } else {
            return TYPE_IMAGE;
        }
    }

    private void checkedImage(ViewHolder holder, FileData fileData) {
        if (mSelectFileData.contains(fileData)) {
            //如果图片已经选中，就取消选中
            unSelectImage(fileData);
            setItemSelect(holder, false);
        } else if (isSingle) {
            //如果是单选，就先清空已经选中的图片，再选中当前图片
            clearImageSelect();
            selectImage(fileData);
            setItemSelect(holder, true);
        } else if (mMaxCount <= 0 || mSelectFileData.size() < mMaxCount) {
            //如果不限制图片的选中数量，或者图片的选中数量
            // 还没有达到最大限制，就直接选中当前图片。
            selectImage(fileData);
            setItemSelect(holder, true);
        }
    }

    /**
     * 选中图片
     *
     * @param fileData
     */
    private void selectImage(FileData fileData) {
        mSelectFileData.add(fileData);
        if (mSelectListener != null) {
            mSelectListener.OnImageSelect(fileData, true, mSelectFileData.size());
        }
    }

    /**
     * 取消选中图片
     *
     * @param fileData
     */
    private void unSelectImage(FileData fileData) {
        mSelectFileData.remove(fileData);
        if (mSelectListener != null) {
            mSelectListener.OnImageSelect(fileData, false, mSelectFileData.size());
        }
    }


    @Override
    public int getItemCount() {
        return useCamera ? getImageCount() + 1 : getImageCount();
    }

    private int getImageCount() {
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

    private FileData getImage(int position) {
        return mFileData.get(useCamera ? position - 1 : position);
    }

    public FileData getFirstVisibleImage(int firstVisibleItem) {
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
    private void setItemSelect(ViewHolder holder, boolean isSelect) {
        if (isSelect) {
            holder.ivSelectIcon.setImageResource(R.drawable.icon_image_select);
            holder.ivMasking.setAlpha(0.5f);
        } else {
            holder.ivSelectIcon.setImageResource(R.drawable.icon_image_un_select);
            holder.ivMasking.setAlpha(0.2f);
        }
    }

    private void clearImageSelect() {
        if (mFileData != null && mSelectFileData.size() == 1) {
            int index = mFileData.indexOf(mSelectFileData.get(0));
            mSelectFileData.clear();
            if (index != -1) {
                notifyItemChanged(useCamera ? index + 1 : index);
            }
        }
    }

    public void setSelectedImages(ArrayList<String> selected) {
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

    public ArrayList<FileData> getSelectImages() {
        return mSelectFileData;
    }

    public void setOnImageSelectListener(OnImageSelectListener listener) {
        this.mSelectListener = listener;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mItemClickListener = listener;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView ivImage;
        ImageView ivSelectIcon;
        ImageView ivMasking;
        ImageView ivGif;
        ImageView ivCamera;

        public ViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_image);
            ivSelectIcon = itemView.findViewById(R.id.iv_select);
            ivMasking = itemView.findViewById(R.id.iv_masking);
            ivGif = itemView.findViewById(R.id.iv_gif);

            ivCamera = itemView.findViewById(R.id.iv_camera);
        }
    }

    public interface OnImageSelectListener {
        void OnImageSelect(FileData fileData, boolean isSelect, int selectCount);
    }

    public interface OnItemClickListener {
        void OnItemClick(FileData fileData, int position);

        void OnCameraClick();
    }
}
