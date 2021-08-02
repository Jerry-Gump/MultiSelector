package com.donkingliang.imageselector.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class IconUtils {
    public static Drawable getDrawable(@NonNull Context context, String iconString, int[] bounds){
        Drawable drawable = getDrawable(context,iconString);

        if (bounds != null && drawable != null){
            drawable.setBounds(bounds[0],bounds[1],bounds[2],bounds[3]);
            Bitmap image = Bitmap.createBitmap(bounds[2],bounds[3],Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(image);
            drawable.draw(cv);
            return new BitmapDrawable(context.getResources(), image);
        }
        return drawable;
    }

    public static Drawable getDrawable(@NonNull Context context, String drawableFile){
        Drawable drawable = null;
        if(drawableFile.startsWith("@drawable:")){
            drawableFile = drawableFile.substring(10);
            if(drawableFile.endsWith(".xml") || drawableFile.endsWith(".png") || drawableFile.endsWith(".jpg")){
                drawableFile = drawableFile.substring(0,drawableFile.length()-4);
            }
            int iconId = getDrawableId(context, drawableFile);
            if(iconId != 0){
                drawable = ContextCompat.getDrawable(context,iconId);
            }
        }
        else if(drawableFile.startsWith("@xml:")){
            drawableFile = drawableFile.substring(5);
            drawable = AssetUtils.getAssetsXmlDrawable(context,drawableFile);
        }else if(drawableFile.startsWith("@file:")){
            drawableFile = drawableFile.substring(6);
            drawable = getFileDrawable(context,drawableFile);
        }
        else {// 默认为asset图片形式
            if(drawableFile.startsWith("@image:")) {
                drawableFile = drawableFile.substring(7);
            }
            drawable = AssetUtils.getAssetsImageDrawable(context,drawableFile);
        }
        return drawable;
    }

    // Drawable工具
    public static Drawable getFileDrawable(@NonNull Context context, String path){
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(path);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        if(inputStream != null){
            return Drawable.createFromResourceStream(context.getResources(), null, inputStream, null);
        }
        return null;
    }

    public static int getDrawableId(@NonNull Context context, String drawableName){
        return context.getResources().getIdentifier(drawableName,"drawable",context.getPackageName());
    }
}
