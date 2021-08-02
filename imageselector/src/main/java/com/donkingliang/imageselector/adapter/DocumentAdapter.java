package com.donkingliang.imageselector.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.donkingliang.imageselector.R;
import com.donkingliang.imageselector.entry.FileData;

public class DocumentAdapter extends SelectorBaseAdapter<DocumentAdapter.ViewHolder> {

    private boolean canPreview;
    /**
     * @param maxCount    图片的最大选择数量，小于等于0时，不限数量，isSingle为false时才有用。
     * @param isSingle    是否单选
     */
    public DocumentAdapter(Context context, int maxCount, boolean isSingle, boolean canPreview) {
        mContext = context;
        this.mInflater = LayoutInflater.from(mContext);
        mMaxCount = maxCount;
        this.isSingle = isSingle;
        this.useCamera = false;
        this.canPreview = canPreview;
    }

    @Override
    public DocumentAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.adapter_documents_item, parent, false);
        return new DocumentAdapter.ViewHolder(view);
    }

    public static boolean isImage(String mime)
    {
        return mime.startsWith("image/");
    }

    public static boolean isVideo(String mime)
    {
        return mime.startsWith("video/");
    }

    public static boolean isAudio(String mime)
    {
        return mime.startsWith("audio/");
    }

    public static boolean isExcel(String mime){
        return mime.contains("vnd.ms-excel") || mime.contains("spreadsheetml.sheet");
    }

    public static boolean isWord(String mime){
        return mime.contains("msword") || mime.contains("wordprocessingml.document");
    }

    public static boolean isPowerPoint(String mime){
        return mime.contains("vnd.ms-powerpoint") || mime.contains("presentationml.presentation");
    }

    public static boolean isText(String mime){
        return mime.startsWith("text/");
    }

    public static boolean isPdf(String mime){
        return mime.endsWith("/pdf");
    }

    @Override
    public void onBindViewHolder(@NonNull DocumentAdapter.ViewHolder holder, int position) {
        final FileData fileData = getFile(position);

        holder.tvTitle.setText(fileData.getName());
        holder.tvTitle.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        /*
        if (holder.tvTitle.getMeasuredWidth() >
                getScreenWidth(mContext) - dip2px(mContext, 24 + 24 + 10 + 12)) {
            holder.tvTitle.setLines(2);
        } else {
            holder.tvTitle.setLines(1);
        }*/

        if (isExcel(fileData.getMimeType())) {
            holder.icFile.setImageResource(R.drawable.vw_ic_excel);
        }
        else if (isWord(fileData.getMimeType())){
            holder.icFile.setImageResource(R.drawable.vw_ic_word);
        }
        else if (isPowerPoint(fileData.getMimeType())){
            holder.icFile.setImageResource(R.drawable.vw_ic_ppt);
        }
        else if (isPdf(fileData.getMimeType())){
            holder.icFile.setImageResource(R.drawable.vw_ic_pdf);
        }
        else if (isText(fileData.getMimeType())){
            holder.icFile.setImageResource(R.drawable.vw_ic_txt);
        }
        else if(isAudio(fileData.getMimeType()))
        {
            holder.icFile.setImageResource(R.drawable.vw_ic_audio);
        }
        else if(isVideo(fileData.getMimeType()))
        {
            holder.icFile.setImageResource(R.drawable.vw_ic_video);
        }
        else{
            holder.icFile.setImageResource(R.drawable.vw_ic_file);
        }

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
        ImageView icFile;

        public ViewHolder(View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tv_title);
            icFile = itemView.findViewById(R.id.ic_file);
        }
    }
}
