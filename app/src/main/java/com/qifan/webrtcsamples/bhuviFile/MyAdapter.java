package com.qifan.webrtcsamples.bhuviFile;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.qifan.webrtcsamples.R;

import java.util.List;

public class MyAdapter extends RecyclerView.Adapter<MyAdapter.MyViewHolder> {

    // Define your data list here
    private List<UsrLst> dataList;

    // Constructor to initialize data list
    public MyAdapter(List<UsrLst> dataList) {
        this.dataList = dataList;
    }

    // Create view holder for each item
    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_layout, parent, false);
        return new MyViewHolder(itemView);
    }

    // Bind data to each item
    @Override
    public void onBindViewHolder(MyViewHolder holder, int position) {
        UsrLst data = dataList.get(position);
        holder.bind(data);
        holder.chckBox.setChecked(data.isChecked());

        holder.chckBox.setOnCheckedChangeListener((compoundButton, isChecked) -> {
            data.setChecked(isChecked);
        });
    }

    // Return total number of items
    @Override
    public int getItemCount() {
        return dataList.size();
    }

    // Define your view holder
    public class MyViewHolder extends RecyclerView.ViewHolder {
        // Define your view elements here
        private TextView textView;
        private CheckBox chckBox;

        public MyViewHolder(View itemView) {
            super(itemView);
            // Initialize your view elements here
            textView = itemView.findViewById(R.id.textView3);
            chckBox = itemView.findViewById(R.id.checkBox);
        }

        // Bind data to views
        public void bind(UsrLst data) {
            textView.setText(data.getUsrName());
        }
    }
}

