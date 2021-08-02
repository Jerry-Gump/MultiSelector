package com.donkingliang.imageselector.adapter;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.donkingliang.imageselector.R;
import com.donkingliang.imageselector.entry.FileData;

public class VideoAdapter extends SelectorBaseAdapter<VideoAdapter.ViewHolder>{

    private boolean canPreview;
    /**
     * @param maxCount    图片的最大选择数量，小于等于0时，不限数量，isSingle为false时才有用。
     * @param isSingle    是否单选
     */
    public VideoAdapter(Context context, int maxCount, boolean isSingle, boolean canPreview) {
        mContext = context;
        this.mInflater = LayoutInflater.from(mContext);
        mMaxCount = maxCount;
        this.isSingle = isSingle;
        this.canPreview = canPreview;
    }

    @Override
    public VideoAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_VIDEO) {
            View view = mInflater.inflate(R.layout.adapter_videos_item, parent, false);
            return new VideoAdapter.ViewHolder(view);
        } else {
            View view = mInflater.inflate(R.layout.adapter_camera, parent, false);
            return new VideoAdapter.ViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_VIDEO) {
            final FileData fileData = getFile(position);
            RequestOptions options = new RequestOptions();
            Glide.with(mContext)
                    .load(isAndroidQ ? fileData.getUri() : fileData.getPath())
                    .apply(options.centerCrop())
                    .transition(withCrossFade())
//                    .transition(new DrawableTransitionOptions().crossFade(500))
                    .into(holder.ivImage);

            setItemSelect(holder, mSelectFileData.contains(fileData));

            //点击选中/取消选中图片
            holder.ivSelectIcon.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    checkedFile(holder, fileData);
                }
            });

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (canPreview) {
                        if (mItemClickListener != null) {
                            int p = holder.getAbsoluteAdapterPosition(); //holder.getAdapterPosition();
                            mItemClickListener.OnItemClick(fileData, useCamera ? p - 1 : p);
                        }
                    } else {
                        checkedFile(holder, fileData);
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
            return TYPE_VIDEO;
        }
    }
    static class ViewHolder extends SelectorBaseAdapter.SelectorViewHolder {

        ImageView ivImage;
        ImageView ivCamera;

        public ViewHolder(View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.iv_image);
            ivCamera = itemView.findViewById(R.id.iv_camera);
        }
    }
}
