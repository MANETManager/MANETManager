package com.example.koichi.manetmanager;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Koichi on 2017/10/31.
 * 合っているかどうかわからじ...
 */

public class MyListAdapter extends ArrayAdapter<String> {
    private LayoutInflater inflater;
    String item;

    //additional
    String[] items;

    //public MyListAdapter(Context context, int resource, List<String> objects) {
    public MyListAdapter(Context context, int resource, String[] objects) {
        super(context, resource, objects);

        //additional
        items = objects;

        inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }
/*
    //実質的な処理はここに
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            //ListViewの各行のレイアウトを設定
            convertView = inflater.inflate(R.layout.list_item, null);
        }

        //リストのアイテムデータの取得
        //item = this.getItem(position);

        //削除ボタンの設定
        Button DeleteButton=(Button)convertView.findViewById(R.id.delete);
        DeleteButton.setOnClickListener(new DeleteListener());

        TextView textView = (TextView) convertView.findViewById(R.id.listItem_username);
        if (textView != null) {
            //アイテムデータに設定されたテキストを表示
            //textView.setText(item);
            textView.setText(items[position]);
        }

        return convertView;
    }
    */

    //削除ボタンのリスナー
    class DeleteListener implements View.OnClickListener {
        public void onClick(View v){

            //削除処理

/*
            //Snackbarで通知
            Snackbar.make(v, "削除しました", Snackbar.LENGTH_LONG)
                    .setAction("Action", null).show();
 */
        }
    }
}
