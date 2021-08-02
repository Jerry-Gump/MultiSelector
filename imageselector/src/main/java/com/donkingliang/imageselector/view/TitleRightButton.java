package com.donkingliang.imageselector.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuBuilder;
import androidx.appcompat.view.menu.MenuPopupHelper;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.appcompat.widget.PopupMenu;

import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.View;

import com.donkingliang.imageselector.utils.IconUtils;

import java.util.List;

public class TitleRightButton extends AppCompatImageButton {
    public static class ButtonAction{
        public String type;
        public String act;
        public List<MenuItem> menuItems;
    }
    public static class MenuItem{
        public String itemId = "";
        public String itemTitle = "";
        public ItemIcon itemIcon = null;
        public ItemClickAction itemClickAction = null;
    }
    public static class ItemIcon{
        public String icon = "";
        public String orientation = "left";
        public int setPadding = 30;
        public int[] setBounds = new int[]{0,0,40,40};
    }
    public static class ItemClickAction{
        public String type = "";
        public String params = "";
        public String extraParams = "";
    }
    public static class ButtonData{
        public String caption;
        public String iconImg;
        public ButtonAction action;
    }
    private Context mContext;
    public TitleRightButton(Context context) {
        super(context);
        mContext = context;
    }

    public TitleRightButton(Context context, AttributeSet attrs) {
        super(context, attrs);
        mContext = context;
    }

    public TitleRightButton(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mContext = context;
    }
    public static Drawable getDrawable( Context context, ItemIcon itemIcon){
        String iconPath = itemIcon.icon;
        int[] setBounds = itemIcon.setBounds;
        return IconUtils.getDrawable(context, iconPath,setBounds);
    }

    public interface ButtonMenuClickListener {
        void OnButtonClick(String actionType, String act);
        void OnMenuClick(String type,String params,String extraParams);
    }
//为了配合json missionActivity
    @SuppressLint("RestrictedApi")
    public void setActionData(@NonNull final ButtonData data, @NonNull final ButtonMenuClickListener listener){
        if(TextUtils.isEmpty(data.iconImg)){
            return;
        }
        setImageDrawable(IconUtils.getDrawable(getContext(),data.iconImg));
        setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v) {
                if(data.action != null){
                    switch (data.action.type){
                        case "popup_menu":
                            PopupMenu popupMenu = new PopupMenu(mContext,TitleRightButton.this);
                            Menu m = popupMenu.getMenu();
                            final List<MenuItem> menuItems = data.action.menuItems;
                            if(menuItems != null && menuItems.size() > 0){
                                for (final MenuItem tmi:
                                        menuItems) {
                                    android.view.MenuItem mi = m.add(Menu.NONE,tmi.itemId.hashCode(),Menu.NONE,tmi.itemTitle);
                                    if(tmi.itemIcon != null){
                                        mi.setIcon(getDrawable(getContext(), tmi.itemIcon));
                                    }
                                    mi.setOnMenuItemClickListener(new android.view.MenuItem.OnMenuItemClickListener() {
                                        @Override
                                        public boolean onMenuItemClick(android.view.MenuItem item) {
                                            if(item.getItemId() == tmi.itemId.hashCode()){
                                                ItemClickAction itemClickAction = tmi.itemClickAction;
                                                if(itemClickAction != null) {
                                                    final String type = itemClickAction.type;
                                                    final String params = itemClickAction.params;
                                                    final String extraParams = itemClickAction.extraParams;
                                                    listener.OnMenuClick(type,params,extraParams);
                                                }
                                            }
                                            return true;
                                        }
                                    });
                                }
                            }
                            MenuPopupHelper menuHelper = new MenuPopupHelper(mContext, (MenuBuilder) popupMenu.getMenu(), TitleRightButton.this);
                            menuHelper.setForceShowIcon(true);
                            menuHelper.show();
                            break;
                        default:
                            listener.OnButtonClick(data.action.type,data.action.act);
                            break;
                    }
                }
            }
        });
    }
}
