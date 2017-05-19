/**
 * Copyright (C) 2016 Hyphenate Inc. All rights reserved.
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.hyphenate.chatuidemo;

import android.app.Application;
import android.content.Context;
import android.support.multidex.MultiDex;
import android.text.TextUtils;

import com.crashlytics.android.Crashlytics;
import com.hyphenate.chat.EMClient;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.utils.EaseUserUtils;
import com.yunzhanghu.redpacketsdk.RPInitRedPacketCallback;
import com.yunzhanghu.redpacketsdk.RPValueCallback;
import com.yunzhanghu.redpacketsdk.RedPacket;
import com.yunzhanghu.redpacketsdk.bean.RedPacketInfo;
import com.yunzhanghu.redpacketsdk.bean.TokenData;
import com.yunzhanghu.redpacketsdk.constant.RPConstant;

import io.fabric.sdk.android.Fabric;


public class DemoApplication extends Application {

    public static Context applicationContext;

    private static DemoApplication instance;
    // login user name
    public final String PREF_USERNAME = "username";

    /**
     * nickname for current user, the nickname instead of ID be shown when user receive notification from APNs
     */
    public static String currentUserNick = "";

    @Override
    public void onCreate() {
        MultiDex.install(this);
        super.onCreate();
        applicationContext = this;
        instance = this;

        //init demo helper
        DemoHelper.getInstance().init(applicationContext);
        //red packet code : 初始化红包上下文，开启日志输出开关
        RedPacket.getInstance().initRedPacket(applicationContext, RPConstant.AUTH_METHOD_EASE_MOB, new RPInitRedPacketCallback() {

            @Override
            public void initTokenData(RPValueCallback<TokenData> callback) {
                TokenData tokenData = new TokenData();
                tokenData.imUserId = EMClient.getInstance().getCurrentUser();
                //此处使用环信id代替了appUserId 开发者可传入App的appUserId
                tokenData.appUserId = EMClient.getInstance().getCurrentUser();
                tokenData.imToken = EMClient.getInstance().getChatConfig().getAccessToken();
                //同步或异步获取TokenData 获取成功后回调onSuccess()方法
                callback.onSuccess(tokenData);
            }

            @Override
            public RedPacketInfo initCurrentUserSync() {
                //这里需要同步设置当前用户id、昵称和头像url
                String currentAvatarUrl = "";
                String currentNickname = EMClient.getInstance().getCurrentUser();
                EaseUser easeUser = EaseUserUtils.getUserInfo(currentNickname);
                if (easeUser != null) {
                    currentAvatarUrl = TextUtils.isEmpty(easeUser.getAvatar()) ? "none" : easeUser.getAvatar();
                    currentNickname = TextUtils.isEmpty(easeUser.getNick()) ? easeUser.getUsername() : easeUser.getNick();
                }
                RedPacketInfo currentUserInfo = new RedPacketInfo();
                currentUserInfo.currentUserId = EMClient.getInstance().getCurrentUser();
                currentUserInfo.currentAvatarUrl = currentAvatarUrl;
                currentUserInfo.currentNickname = currentNickname;
                return currentUserInfo;
            }
        });
        RedPacket.getInstance().setDebugMode(true);
        //end of red packet code
    }

    public static DemoApplication getInstance() {
        return instance;
    }

    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        MultiDex.install(this);
    }
}
