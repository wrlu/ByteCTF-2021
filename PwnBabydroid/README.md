### babydroid | SOLVED | working : andrea & wrlu
- Verified on local Pixel phone successfully, but the game server is down, exp：https://wrlus.com/wp-content/uploads/bytectf/babydroid/app-debug.apk

- very easy bug, any intent pass to the startActivity
```java=
public class Vulnerable extends Activity {
    /* access modifiers changed from: protected */
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        startActivity((Intent) getIntent().getParcelableExtra("intent"));
    }
}
```
- the flag is in the local file storage and it has a fileprovider inside
```xml=
<provider android:name="androidx.core.content.FileProvider" android:exported="false" android:authorities="androidx.core.content.FileProvider" android:grantUriPermissions="true">
            <meta-data android:name="android.support.FILE_PROVIDER_PATHS" android:resource="@xml/file_paths"/>
        </provider>

```
- so we can use the fileprovider to read flag file
- https://bytedance.feishu.cn/file/boxcnWibqpknk3S708qerqHoxiP, slide 12
- use `Intent.FLAG_GRANT_READ_URI_PERMISSION`
- Bug: The cloud emulator starts our app use `am start` command but does not use action parameter, this will be default `android.intent.action.MAIN` in local but is null on cloud, so our poc causes a NPE.
- SOLVED: ByteCTF{ee169e20-17be-4b22-9a71-d0fe687eced9}