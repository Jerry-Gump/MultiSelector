package com.donkingliang.imageselector.utils;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class AssetUtils {
    /**
     * 从Assets中读取图片
     */
    public static Bitmap getAssetsBitmap(Context context, String fileName) {
        Bitmap image = null;
        AssetManager am = context.getResources().getAssets();
        try {
            InputStream is = am.open(fileName);
            image = BitmapFactory.decodeStream(is);
            is.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return image;

    }

    /**
     * 从Assets中读取文件转成字符串，文件大小最大1M
     */
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public static String getAssetsString(@NonNull Context context,
                                         String fileName) {
        AssetManager am = context.getResources().getAssets();
        StringBuilder builder = new StringBuilder();//字符串变量，多次赋值更快
        try {
            InputStream is = am.open(fileName);
            InputStreamReader inputStreamReader = new InputStreamReader(is, StandardCharsets.UTF_8);
            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
            String content;
            while ((content = bufferedReader.readLine()) != null) {
                builder.append(content);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return builder.toString();
    }

    public static InputStream getAssetsStream(@NonNull Context context, String fileName){
        AssetManager am = context.getResources().getAssets();
        try {
            InputStream is = am.open(fileName);
            return is;
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

    public static XmlPullParser getAssetXml(@NonNull Context context, String filePath){
        AssetManager assetManager = context.getResources().getAssets();
        try {
            XmlPullParser p = assetManager.openXmlResourceParser(filePath);
            return p;
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }

    public static Drawable getAssetsImageDrawable(@NonNull Context context, String fileName){
        InputStream inputStream = getAssetsStream(context,fileName);
        if(inputStream != null){
            Drawable rslt = Drawable.createFromResourceStream(context.getResources(), null, inputStream, null);
            return  rslt;
        }
        return null;
    }

    public static Drawable getAssetsXmlDrawable(@NonNull Context context,String fileName){
        XmlPullParser xp = getAssetXml(context,fileName);
        if(xp != null){
            try {
                Drawable rslt = Drawable.createFromXml(context.getResources(), xp);
                return rslt;
            }catch (IOException | XmlPullParserException e){
                e.printStackTrace();
            }
        }
        return null;
    }
}
