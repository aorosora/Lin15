package com.xp.legend.lin15.hooks;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AndroidAppHelper;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.renderscript.ScriptGroup;
import android.support.v4.app.Fragment;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class SettingsActivityHook implements IXposedHookLoadPackage {

    private static final String CLASS="com.android.launcher3.SettingsActivity$LauncherSettingsFragment";
    private static final String METHOD="onCreate";

    private SharedPreferences sharedPreferences;
    private static final String SHARED="launch_shared";
    private static final String PASS="pass_hide_icon";
    private LinearLayoutManager linearLayoutManager;



    @Override
    public void handleLoadPackage(XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {

        if (!lpparam.packageName.equals("org.lineageos.trebuchet")){

//            XposedBridge.log("lll--sss-->>"+ lpparam.packageName);
            return;
        }



//        XposedHelpers.findAndHookConstructor(CLASS, lpparam.classLoader, Bundle.class, new XC_MethodHook() {
//            @Override
//            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
//                super.afterHookedMethod(param);
//
//                XposedBridge.log("lll--sss-->>"+ Arrays.toString(param.args));
//
////                if (settingReceiver==null){
////
////                    settingReceiver=new SettingReceiver();
////                    IntentFilter intentFilter=new IntentFilter();
////
////                    AndroidAppHelper.currentApplication().registerReceiver(settingReceiver,intentFilter);
////                }
//
//            }
//        });

        XposedHelpers.findAndHookMethod(CLASS, lpparam.classLoader, METHOD, Bundle.class, new XC_MethodHook() {
            @Override
            protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                super.afterHookedMethod(param);



                PreferenceFragment fragment= (PreferenceFragment) param.thisObject;

                PreferenceScreen preferenceScreen= fragment.getPreferenceScreen();

                Preference preference=new Preference(fragment.getContext());

                preference.setKey("hide_icon");
                preference.setTitle("隐藏应用");
                preference.setSummary("隐藏那些……你懂的~");

                preferenceScreen.addPreference(preference);

                preference.setOnPreferenceClickListener(preference1 ->{

//                    Toast.makeText(fragment.getContext(), "隐藏应用", Toast.LENGTH_SHORT).show();

                    checkPassIfExits(fragment.getActivity());

                 return true;
                });

                sharedPreferences=fragment.getActivity().getSharedPreferences(SHARED,Context.MODE_PRIVATE);

            }
        });

    }

    /**
     * 需要密码或是设置密码
     */
    private void checkPassIfExits(Activity activity){

        String pass=sharedPreferences.getString(PASS,"");

        if (pass.isEmpty()){//如果密码是空的，设置密码


            showSettingPassDialog(activity);

        }else {//密码不是空的，弹出输入框输入密码

            showConfirmPassDialog(activity);

        }

    }

    /**
     * 设置密码界面
     */
    private void showSettingPassDialog(Activity activity){

        AlertDialog.Builder builder=new AlertDialog.Builder(activity);

        EditText editText=new EditText(activity);
        editText.setInputType(InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        builder.setPositiveButton("下一步",(dialogInterface, i) -> {

            String pass=editText.getText().toString();

            if (pass.isEmpty()){
                return;
            }

            confirmPassDialog(activity,pass);
        });

        builder.setView(editText).setTitle("请设置密码");

        builder.show();

    }

    /**
     * 确认密码
     * @param activity
     * @param pass
     */
    private void confirmPassDialog(Activity activity,String pass){


        AlertDialog.Builder builder=new AlertDialog.Builder(activity);

        EditText editText=new EditText(activity);
        editText.setInputType(InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        builder.setPositiveButton("确认",(dialogInterface, i) -> {

            String confirm_pass=editText.getText().toString();


            if (pass.equals(confirm_pass)){

                savePass(confirm_pass);

            }else {

                Toast.makeText(activity, "密码不一致", Toast.LENGTH_SHORT).show();
            }

        });

        builder.setView(editText).setTitle("请确认密码");

        builder.show();

    }

    //md5加密改名
    private String getMd5(String plainText) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(plainText.getBytes());
            byte b[] = md.digest();

            int i;

            StringBuffer buf = new StringBuffer("");
            for (int offset = 0; offset < b.length; offset++) {
                i = b[offset];
                if (i < 0)
                    i += 256;
                if (i < 16)
                    buf.append("0");
                buf.append(Integer.toHexString(i));
            }
            //32位加密
            return buf.toString();
            // 16位的加密
            //return buf.toString().substring(8, 24);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }

    }

    /**
     * 保存密码
     * @param pass
     */
    private void savePass(String pass){

        String save_pass=getMd5(pass);

        sharedPreferences.edit().putString(PASS,save_pass).apply();//保存

    }


    /**
     *
     * 已有密码，弹出输入密码框
     * @param activity
     */
    private void showConfirmPassDialog(Activity activity){

        AlertDialog.Builder builder=new AlertDialog.Builder(activity);

        EditText editText=new EditText(activity);

        editText.setInputType(InputType.TYPE_NUMBER_VARIATION_PASSWORD);

        builder.setPositiveButton("确定",(dialogInterface, i) -> {
            String pass=editText.getText().toString();

            confirmPass(activity,pass);

        });

        builder.setView(editText).setTitle("请输入密码");

        builder.show();

    }

    /**
     * 输入密码并确认
     * @param pass
     */
    private void confirmPass(Activity activity,String pass){

        String con_pass=getMd5(pass);

        if (con_pass==null){
            Toast.makeText(AndroidAppHelper.currentApplication(), "密码错误", Toast.LENGTH_SHORT).show();
            return;
        }

        String save_pass=sharedPreferences.getString(PASS,"");

        if (con_pass.equals(save_pass)){

            showHideApps(activity);
        }else {

            Toast.makeText(AndroidAppHelper.currentApplication(), "密码错误", Toast.LENGTH_SHORT).show();
        }

    }

    /**
     * 展示已隐藏APP
     * @param activity
     */
    private void showHideApps(Activity activity){

        AlertDialog.Builder builder=new AlertDialog.Builder(activity);

        RecyclerView recyclerView=new RecyclerView(activity);
        if (linearLayoutManager==null){
            linearLayoutManager=new LinearLayoutManager(activity);
        }

        HideIconAdapter adapter=new HideIconAdapter();
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setAdapter(adapter);

        builder.setTitle("已隐藏的APP").setView(recyclerView).show();

    }

    class IconPackInfo{

        String packageName;
        CharSequence label;
        Drawable icon;

        public IconPackInfo(String packageName, CharSequence label, Drawable icon) {
            this.packageName = packageName;
            this.label = label;
            this.icon = icon;
        }

    }

    class HideIconAdapter extends RecyclerView.Adapter<HideIconAdapter.ViewHolder>{

        int image_id=0;
        int text_id=0;

        private List<IconPackInfo> infoList;

        public void setInfoList(List<IconPackInfo> infoList) {
            this.infoList = infoList;
            notifyDataSetChanged();
        }

        @Override
        public HideIconAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

            View view=createView(parent.getContext());

            ViewHolder viewHolder=new ViewHolder(view);

            viewHolder.view.setOnClickListener(view1 -> {

                int position=viewHolder.getAdapterPosition();
                IconPackInfo info=infoList.get(position);
                Toast.makeText(parent.getContext(), ""+info.label, Toast.LENGTH_SHORT).show();

            });

            return viewHolder;
        }

        @Override
        public void onBindViewHolder(HideIconAdapter.ViewHolder holder, int position) {

            if (infoList==null){
                return;
            }

            IconPackInfo info=infoList.get(position);

            holder.imageView.setImageDrawable(info.icon);

            holder.textView.setText(info.label);

        }

        @Override
        public int getItemCount() {

            if (infoList!=null){
                return infoList.size();
            }
            return 0;
        }

        class ViewHolder extends RecyclerView.ViewHolder {

            ImageView imageView;
            View view;
            TextView textView;

            public ViewHolder(View itemView) {
                super(itemView);
                view=itemView;
                this.imageView=itemView.findViewById(image_id);
                this.textView=itemView.findViewById(text_id);
            }
        }

        private View createView(Context context){

            int dp16= getValue(16,context.getResources());

            int dp72=getValue(72,context.getResources());

            int dp48=getValue(48,context.getResources());

            int dp12=getValue(12,context.getResources());

            LinearLayout linearLayout=new LinearLayout(context);
            LinearLayout.LayoutParams params= new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,dp72);

            linearLayout.setHorizontalGravity(LinearLayout.HORIZONTAL);//横向布局

            linearLayout.setLayoutParams(params);
            linearLayout.setGravity(Gravity.CENTER);

            ImageView imageView=new ImageView(context);

            LinearLayout.LayoutParams imageParams=new LinearLayout.LayoutParams(dp48,dp48);

            imageParams.setMargins(dp16,dp12,dp16,dp12);

            imageView.setLayoutParams(imageParams);

            int image_id=View.generateViewId();

            imageView.setId(image_id);

            this.image_id=image_id;//外传ID

            TextView textView=new TextView(context);

            LinearLayout.LayoutParams textParams=new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            textParams.rightMargin=dp16;

            textView.setLayoutParams(textParams);

            textView.setMaxLines(1);

            textView.setEllipsize(TextUtils.TruncateAt.END);

            text_id=View.generateViewId();

            textView.setId(text_id);

            linearLayout.addView(imageView);
            linearLayout.addView(textView);

            return linearLayout;

        }

        private int getValue(int a, Resources resources){

            return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,a,resources.getDisplayMetrics());
        }
    }
}
