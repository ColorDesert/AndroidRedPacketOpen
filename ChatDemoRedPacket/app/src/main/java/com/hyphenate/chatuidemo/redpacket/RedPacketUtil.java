package com.hyphenate.chatuidemo.redpacket;

import android.app.ProgressDialog;
import android.content.Context;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.widget.Toast;

import com.hyphenate.EMCallBack;
import com.hyphenate.chat.EMClient;
import com.hyphenate.chat.EMCmdMessageBody;
import com.hyphenate.chat.EMGroup;
import com.hyphenate.chat.EMMessage;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.easeui.EaseConstant;
import com.hyphenate.easeui.domain.EaseUser;
import com.hyphenate.easeui.utils.EaseUserUtils;
import com.hyphenate.easeui.widget.EaseChatMessageList;
import com.hyphenate.exceptions.HyphenateException;
import com.yunzhanghu.redpacketsdk.RPGroupMemberListener;
import com.yunzhanghu.redpacketsdk.RPSendPacketCallback;
import com.yunzhanghu.redpacketsdk.RPValueCallback;
import com.yunzhanghu.redpacketsdk.RedPacket;
import com.yunzhanghu.redpacketsdk.bean.RPUserBean;
import com.yunzhanghu.redpacketsdk.bean.RedPacketInfo;
import com.yunzhanghu.redpacketsdk.constant.RPConstant;
import com.yunzhanghu.redpacketui.utils.RPRedPacketUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by max on 16/5/24
 */
public class RedPacketUtil {

    /**
     * 进入红包页面的相关方法
     *
     * @param activity   FragmentActivity
     * @param itemType   单聊红包、群聊红包、小额随机红包
     * @param receiverId 接收者id或者群id
     * @param callback   RPSendPacketCallback
     */
    public static void startRedPacket(final FragmentActivity activity, int itemType, final String receiverId, RPSendPacketCallback callback) {
        RedPacketInfo redPacketInfo = new RedPacketInfo();
        if (itemType == RPConstant.RP_ITEM_TYPE_GROUP) {
            //拉取最新群组数据
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        EMClient.getInstance().groupManager().getGroupFromServer(receiverId);
                    } catch (HyphenateException e) {
                        e.printStackTrace();
                    }
                }
            }).start();
            RedPacket.getInstance().setRPGroupMemberListener(new RPGroupMemberListener() {
                @Override
                public void getGroupMember(String groupId, RPValueCallback<List<RPUserBean>> callback) {
                    EMGroup group = EMClient.getInstance().groupManager().getGroup(groupId);
                    List<String> members = group.getMembers();
                    List<RPUserBean> userBeanList = new ArrayList<>();
                    EaseUser user;
                    for (int i = 0; i < members.size(); i++) {
                        RPUserBean userBean = new RPUserBean();
                        userBean.userId = members.get(i);
                        if (userBean.userId.equals(EMClient.getInstance().getCurrentUser())) {
                            continue;
                        }
                        user = EaseUserUtils.getUserInfo(userBean.userId);
                        if (user != null) {
                            userBean.userAvatar = TextUtils.isEmpty(user.getAvatar()) ? "none" : user.getAvatar();
                            userBean.userNickname = TextUtils.isEmpty(user.getNick()) ? user.getUsername() : user.getNick();
                        } else {
                            userBean.userNickname = userBean.userId;
                            userBean.userAvatar = "none";
                        }
                        userBeanList.add(userBean);
                    }
                    callback.onSuccess(userBeanList);
                }
            });
            EMGroup group = EMClient.getInstance().groupManager().getGroup(receiverId);
            redPacketInfo.groupId = group.getGroupId();
            redPacketInfo.groupMemberCount = group.getAffiliationsCount();
        } else {
            EaseUser easeToUser = EaseUserUtils.getUserInfo(receiverId);
            String receiverAvatarUrl = "none";
            String receiverNickname = "";
            if (easeToUser != null) {
                receiverAvatarUrl = TextUtils.isEmpty(easeToUser.getAvatar()) ? "none" : easeToUser.getAvatar();
                receiverNickname = TextUtils.isEmpty(easeToUser.getNick()) ? easeToUser.getUsername() : easeToUser.getNick();
            }
            redPacketInfo.receiverId = receiverId;
            redPacketInfo.receiverAvatarUrl = receiverAvatarUrl;
            redPacketInfo.receiverNickname = receiverNickname;
        }
        RPRedPacketUtil.getInstance().startRedPacket(activity, itemType, redPacketInfo, callback);
    }


    public static EMMessage createRPMessage(Context context, RedPacketInfo redPacketInfo, String receiverId) {
        EMMessage message = EMMessage.createTxtSendMessage("[" + context.getResources().getString(R.string.easemob_red_packet) + "]" + redPacketInfo.redPacketGreeting, receiverId);
        message.setAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_MESSAGE, true);
        message.setAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_SPONSOR_NAME, context.getResources().getString(R.string.easemob_red_packet));
        message.setAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_GREETING, redPacketInfo.redPacketGreeting);
        message.setAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_ID, redPacketInfo.redPacketId);
        message.setAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_TYPE, redPacketInfo.redPacketType);
        return message;
    }

    /**
     * 拆红包的方法
     *
     * @param activity    FragmentActivity
     * @param chatType    聊天类型
     * @param message     EMMessage
     * @param receiverId  消息接收者id
     * @param messageList EaseChatMessageList
     */
    public static void openRedPacket(final FragmentActivity activity, final int chatType, final EMMessage message, final String receiverId, final EaseChatMessageList messageList) {
        final ProgressDialog progressDialog = new ProgressDialog(activity);
        progressDialog.setCanceledOnTouchOutside(false);
        String redPacketId = message.getStringAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_ID, "");
        String redPacketType = message.getStringAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_TYPE, "");
        RPRedPacketUtil.getInstance().openRedPacket(redPacketId, redPacketType, activity, new RPRedPacketUtil.RPOpenPacketCallback() {
            @Override
            public void onSuccess(RedPacketInfo redPacketInfo) {
                //领取红包成功 发送消息到聊天窗口
                if (chatType == EaseConstant.CHATTYPE_SINGLE) {
                    if (!isRandomRedPacket(message)) {
                        EMMessage msg = EMMessage.createTxtSendMessage(String.format(activity.getResources().getString(R.string.msg_someone_take_red_packet), redPacketInfo.receiverNickname), receiverId);
                        msg.setAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_ACK_MESSAGE, true);
                        msg.setAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_RECEIVER_NICKNAME, redPacketInfo.receiverNickname);
                        msg.setAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_SENDER_NICKNAME, redPacketInfo.senderNickname);
                        EMClient.getInstance().chatManager().sendMessage(msg);
                    }
                } else {
                    sendRedPacketAckMessage(message, redPacketInfo.senderId, redPacketInfo.senderNickname, redPacketInfo.receiverId, redPacketInfo.receiverNickname, new EMCallBack() {
                        @Override
                        public void onSuccess() {
                            messageList.refresh();
                        }

                        @Override
                        public void onError(int i, String s) {

                        }

                        @Override
                        public void onProgress(int i, String s) {

                        }
                    });
                }
            }

            @Override
            public void showLoading() {
                progressDialog.show();
            }

            @Override
            public void hideLoading() {
                progressDialog.dismiss();
            }

            @Override
            public void onError(String code, String message) {
                Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
            }
        });
    }


    /**
     * 使用cmd消息发送领到红包之后的回执消息
     */
    private static void sendRedPacketAckMessage(final EMMessage message, final String senderId, final String senderNickname, String receiverId, final String receiverNickname, final EMCallBack callBack) {
        //创建透传消息
        final EMMessage cmdMsg = EMMessage.createSendMessage(EMMessage.Type.CMD);
        cmdMsg.setChatType(EMMessage.ChatType.Chat);
        EMCmdMessageBody cmdBody = new EMCmdMessageBody(RPConstant.REFRESH_RED_PACKET_ACK_ACTION);
        cmdMsg.addBody(cmdBody);
        cmdMsg.setReceipt(senderId);
        //设置扩展属性
        cmdMsg.setAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_ACK_MESSAGE, true);
        cmdMsg.setAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_SENDER_NICKNAME, senderNickname);
        cmdMsg.setAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_RECEIVER_NICKNAME, receiverNickname);
        cmdMsg.setAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_SENDER_ID, senderId);
        cmdMsg.setAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_RECEIVER_ID, receiverId);
        cmdMsg.setAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_GROUP_ID, message.getTo());
        cmdMsg.setMessageStatusCallback(new EMCallBack() {
            @Override
            public void onSuccess() {
                //保存消息到本地
                EMMessage sendMessage = EMMessage.createTxtSendMessage("content", message.getTo());
                sendMessage.setChatType(EMMessage.ChatType.GroupChat);
                sendMessage.setFrom(message.getFrom());
                sendMessage.setTo(message.getTo());
                sendMessage.setMsgId(UUID.randomUUID().toString());
                sendMessage.setMsgTime(cmdMsg.getMsgTime());
                sendMessage.setUnread(false);//去掉未读的显示
                sendMessage.setDirection(EMMessage.Direct.SEND);
                sendMessage.setAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_ACK_MESSAGE, true);
                sendMessage.setAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_SENDER_NICKNAME, senderNickname);
                sendMessage.setAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_RECEIVER_NICKNAME, receiverNickname);
                sendMessage.setAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_SENDER_ID, senderId);
                EMClient.getInstance().chatManager().saveMessage(sendMessage);
                callBack.onSuccess();
            }

            @Override
            public void onError(int i, String s) {

            }

            @Override
            public void onProgress(int i, String s) {

            }
        });
        EMClient.getInstance().chatManager().sendMessage(cmdMsg);
    }

    /**
     * 使用cmd消息收取领到红包之后的回执消息
     */
    public static void receiveRedPacketAckMessage(EMMessage message) {
        String senderNickname = message.getStringAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_SENDER_NICKNAME, "");
        String receiverNickname = message.getStringAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_RECEIVER_NICKNAME, "");
        String senderId = message.getStringAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_SENDER_ID, "");
        String receiverId = message.getStringAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_RECEIVER_ID, "");
        String groupId = message.getStringAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_GROUP_ID, "");
        String currentUser = EMClient.getInstance().getCurrentUser();
        //更新UI为 xx领取了你的红包
        if (currentUser.equals(senderId) && !receiverId.equals(senderId)) {//如果不是自己领取的红包更新此类消息UI
            EMMessage msg = EMMessage.createTxtSendMessage("content", groupId);
            msg.setChatType(EMMessage.ChatType.GroupChat);
            msg.setFrom(message.getFrom());
            if (TextUtils.isEmpty(groupId)) {
                msg.setTo(message.getTo());
            } else {
                msg.setTo(groupId);
            }
            msg.setMsgId(UUID.randomUUID().toString());
            msg.setMsgTime(message.getMsgTime());
            msg.setDirection(EMMessage.Direct.RECEIVE);
            msg.setUnread(false);//去掉未读的显示
            msg.setAttribute(RPConstant.MESSAGE_ATTR_IS_RED_PACKET_ACK_MESSAGE, true);
            msg.setAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_SENDER_NICKNAME, senderNickname);
            msg.setAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_RECEIVER_NICKNAME, receiverNickname);
            msg.setAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_SENDER_ID, senderId);
            //保存消息
            EMClient.getInstance().chatManager().saveMessage(msg);
        }
    }


    public static boolean isADRedPacket(EMMessage message) {
        String redPacketType = message.getStringAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_TYPE, "");
        return !TextUtils.isEmpty(redPacketType) && redPacketType.equals(RPConstant.RED_PACKET_TYPE_ADVERTISEMENT);
    }

    public static boolean isRandomRedPacket(EMMessage message) {
        String redPacketType = message.getStringAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_TYPE, "");
        return !TextUtils.isEmpty(redPacketType) && redPacketType.equals(RPConstant.RED_PACKET_TYPE_SINGLE_RANDOM);
    }
}
