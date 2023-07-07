package cn.edu.bistu.musicplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

public class SongListAdapter extends ArrayAdapter<Song> {
    private final int resourceId;

    public SongListAdapter(@NonNull Context context, int resource, @NonNull List<Song> objects) {
        super(context, resource, objects);
        resourceId = resource;
    }

    @SuppressLint("SetTextI18n")
    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        Song song = getItem(position);    //获取当前项的Song实例
        //提升ListView的运行效率：不会重复加载布局，对控件的实例进行缓存。
        View view;
        ViewHolder viewHolder;
        if (convertView == null) {
            view = LayoutInflater.from(getContext()).inflate(resourceId, parent, false);
            viewHolder = new ViewHolder();
            viewHolder.index = view.findViewById(R.id.index);
            viewHolder.name = view.findViewById(R.id.name);
            view.setTag(viewHolder);    //将ViewHolder存储在view中
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }
        viewHolder.index.setText(position + "");
        viewHolder.name.setText(song.getName());
        return view;
    }

    private static class ViewHolder {
        TextView index;
        TextView name;
    }
}
