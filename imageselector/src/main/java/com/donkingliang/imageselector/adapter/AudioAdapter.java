package com.donkingliang.imageselector.adapter;

import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.donkingliang.imageselector.R;
import com.donkingliang.imageselector.entry.FileData;

public class AudioAdapter extends SelectorBaseAdapter<AudioAdapter.ViewHolder> {

    private boolean canPreview;
    /**
     * @param maxCount    图片的最大选择数量，小于等于0时，不限数量，isSingle为false时才有用。
     * @param isSingle    是否单选
     */
    public AudioAdapter(Context context, int maxCount, boolean isSingle, boolean canPreview) {
        mContext = context;
        this.mInflater = LayoutInflater.from(mContext);
        mMaxCount = maxCount;
        this.isSingle = isSingle;
        this.useCamera = false;
        this.canPreview = canPreview;
    }

    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.adapter_audios_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        final FileData fileData = getFile(position);

        holder.tvDuration.setText(getDurationString(fileData.getDuration()));
        holder.tvDuration.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        holder.tvTitle.setText(fileData.getName());
        holder.tvTitle.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        /*
        if (holder.tvTitle.getMeasuredWidth() >
                getScreenWidth(mContext) - dip2px(mContext, 10 + 24 + 24 + 10 * 2 )) {
            holder.tvTitle.setLines(2);
        } else {
            holder.tvTitle.setLines(1);
        }*/
        
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
                        int p = holder.getAbsoluteAdapterPosition();// holder.getAdapterPosition();
                        mItemClickListener.OnItemClick(fileData, p);
                    }
                } else {
                    checkedFile(holder, fileData);
                }
            }
        });
    }
    @Override
    public int getItemViewType(int position) {
        return TYPE_AUDIO;
    }
    static class ViewHolder extends SelectorBaseAdapter.SelectorViewHolder {
        TextView tvTitle;
        TextView tvDuration;

        public ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            tvDuration = itemView.findViewById(R.id.tv_duration);
        }
    }
}
