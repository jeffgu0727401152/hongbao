package com.asus.jeff.hongbao;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.PendingIntent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.List;


public class HongbaoService extends AccessibilityService {

    private AccessibilityNodeInfo mEventNodeInfo = null;
    private boolean mFromNotification;
    private boolean mLuckyMoneyPicked;
    private boolean mNewPacketReceived;

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {

        final int eventType = event.getEventType();
        this.mEventNodeInfo = null;

        switch (eventType) {
            case AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED:// 通知栏事件
                List<CharSequence> texts = event.getText();
                if (!texts.isEmpty()) {
                    for (CharSequence text : texts) {
                        String content = text.toString();
                        if (content.contains("[微信红包]")) {
                            // 监听到微信红包的notification，打开通知
                            if (event.getParcelableData() != null
                                    && event.getParcelableData() instanceof Notification) {
                                Notification notification = (Notification) event
                                        .getParcelableData();
                                PendingIntent pendingIntent = notification.contentIntent;
                                try {
                                    pendingIntent.send();
                                    mFromNotification = true;
                                } catch (PendingIntent.CanceledException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                    }
                }
                break;
            case AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED:
            case AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED:
                String className = event.getClassName().toString();
                if (className.equals("com.tencent.mm.ui.LauncherUI")
                        || className.equals("android.widget.RelativeLayout")) {
                    Log.e("hongbao","======findRedPacket=====");
                    mLuckyMoneyPicked = false;
                    mEventNodeInfo = event.getSource();
                    findPacket();// 找到界面红包并点击
                } else if (className
                        .equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyReceiveUI")) {
                    //点击红包后出现的弹窗
                    if (mNewPacketReceived) {
                        if (!openPacket()) {
                            performGlobalAction(GLOBAL_ACTION_BACK);
                        }
                    }
                } else if (className
                        .equals("com.tencent.mm.plugin.luckymoney.ui.LuckyMoneyDetailUI")) {
                    //领取成功，已经领取红包再点击的详情页面
                    if (mLuckyMoneyPicked = true) {
                        performGlobalAction(GLOBAL_ACTION_BACK);//红包详情页面返回
                    }
                }
                break;
        }
    }

    private boolean openPacket() {
        boolean ret = false;
        AccessibilityNodeInfo nodeInfo = getRootInActiveWindow();

        if (nodeInfo != null) {
            Log.e("hongbao","----openPacket----");
            List<AccessibilityNodeInfo> nodeInfos = nodeInfo
                    .findAccessibilityNodeInfosByText("拆红包");
            List<AccessibilityNodeInfo> nodefail1 = nodeInfo
                    .findAccessibilityNodeInfosByText("红包详情");
            List<AccessibilityNodeInfo> nodefail2 = nodeInfo
                    .findAccessibilityNodeInfosByText("手慢了");

            if (!nodefail1.isEmpty() || !nodefail2.isEmpty()) {
                Log.e("hongbao","----Packet fail----");
                ret = false;
            } else {
                int size = nodeInfos.size();
                if (size > 0) {
                    AccessibilityNodeInfo cellNode = nodeInfos.get(size - 1);
                    Log.e("hongbao","----Packet sucess----");
                    cellNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    ret = true;
                }
            }
        }
        mLuckyMoneyPicked = true;
        mNewPacketReceived = false;
        return ret;
    }

    private void findPacket() {

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();

        if (rootNode != null) {

            List<AccessibilityNodeInfo> nodeInfos = rootNode
                    .findAccessibilityNodeInfosByText("领取红包");

            int size = nodeInfos.size();
            if (size>0) {

                AccessibilityNodeInfo cellNode = nodeInfos.get(size-1);

                Log.e("eventSoucre", mEventNodeInfo.toString());
                Log.e("rootNode", rootNode.toString());

                if (!mFromNotification) {
                    if (rootNode.equals(mEventNodeInfo)) {
                        mNewPacketReceived = false;
                        mFromNotification = false;
                        Log.e("hongbao", "-----event from whole activity,ignore!");
                        return;
                    }
                }

                mNewPacketReceived = true;
                mFromNotification = false;
                Log.e("hongbao","-----check ok, click!");
                cellNode.getParent().performAction(AccessibilityNodeInfo.ACTION_CLICK);
            }
        }
    }

    @Override
    public void onInterrupt() {

    }

}
