package com.viasofts.mygcs;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class Adapter extends RecyclerView.Adapter<Holder> {
    ArrayList<String> recyclerlist;
    Adapter(ArrayList<String> recyclerlist) {
        this.recyclerlist = recyclerlist; }

        @NonNull
        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View view = inflater.inflate(R.layout.item_recycler, parent, false);
        return new Holder(view);
    }
    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        holder.tv.setText(recyclerlist.get(position));
    }
    @Override
    public int getItemCount() {
        return recyclerlist.size();
    }
}
class Holder extends RecyclerView.ViewHolder {
    TextView tv;
    public Holder(@NonNull View itemView) {
        super(itemView);
        tv = itemView.findViewById(R.id.text);
    }
}

