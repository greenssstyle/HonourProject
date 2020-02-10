package com.example.thinkpad.voiceassistant.ui;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.example.thinkpad.voiceassistant.R;

import java.util.List;

public class TextAdapter extends BaseAdapter {

    private List<ListData> lists;
    private Context mContext;


    public TextAdapter(List<ListData> lists, Context mContext) {
        this.lists = lists;
        this.mContext = mContext;
    }


    @Override
    public int getCount() {
        return lists.size();
    }

    @Override
    public Object getItem(int position) {
        return lists.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder = null;
        if (convertView == null){
            holder = new ViewHolder();
            convertView = View.inflate(mContext, R.layout.activity_item_left,null);
            holder.content = convertView.findViewById(R.id.itemleft_tv);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
            if (lists.get(position).getFlag() == ListData.SEND){
                convertView = View.inflate(mContext,R.layout.activity_item_right,null);
                holder.content = convertView.findViewById(R.id.itemright_tv);
                convertView.setTag(holder);
            } else {
                convertView = View.inflate(mContext,R.layout.activity_item_left,null);
                holder.content =  convertView.findViewById(R.id.itemleft_tv);
                convertView.setTag(holder);
            }
        }

        ListData listData=lists.get(position);
        if (listData.getFlag()==ListData.RECEIVER){
            holder.content.setText(listData.getContent());
        } else {
            holder.content.setText("\""+lists.get(position).getContent()+"\"");
        }
        return convertView;
    }

    private class ViewHolder{
        private TextView content;
    }
}
