package com.hyphenate.chatuidemo.redpacket;

import android.content.Context;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.hyphenate.chat.EMMessage;
import com.hyphenate.chatuidemo.R;
import com.hyphenate.easeui.widget.chatrow.EaseChatRow;
import com.yunzhanghu.redpacketsdk.constant.RPConstant;

public class ChatRowADRedPacket extends EaseChatRow {

    private TextView mTvGreeting;
    private TextView mTvSponsorName;

    public ChatRowADRedPacket(Context context, EMMessage message, int position, BaseAdapter adapter) {
        super(context, message, position, adapter);
    }

    @Override
    protected void onInflateView() {
        if (RedPacketUtil.isADRedPacket(message)) {
            inflater.inflate(message.direct() == EMMessage.Direct.RECEIVE ?
                    R.layout.em_row_received_ad_packet : R.layout.em_row_sent_ad_packet, this);
        }
    }

    @Override
    protected void onFindViewById() {
        mTvGreeting = (TextView) findViewById(R.id.tv_money_greeting);
        mTvSponsorName = (TextView) findViewById(R.id.tv_sponsor_name);
    }

    @Override
    protected void onSetUpView() {
        String sponsorName = message.getStringAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_SPONSOR_NAME, "");
        String greetings = message.getStringAttribute(RPConstant.MESSAGE_ATTR_RED_PACKET_GREETING, "");
        mTvSponsorName.setText(String.format("%s的现金红包", sponsorName));
        mTvGreeting.setText(greetings);
    }

    @Override
    protected void onUpdateView() {
        adapter.notifyDataSetChanged();
    }

    @Override
    protected void onBubbleClick() {
    }

}
