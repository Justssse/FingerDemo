package com.finger.demo;

import android.hardware.fingerprint.Fingerprint;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class FingerPrintAdapter extends RecyclerView.Adapter<FingerPrintAdapter.MyViewHolder> {

    private List<Fingerprint> fingerprintList = new ArrayList<>(5);
    private ClickCallBack callBack;

    static class MyViewHolder extends RecyclerView.ViewHolder{

        TextView tvFingerprintName;
        Button btnDelete;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);
            tvFingerprintName = (TextView) itemView.findViewById(R.id.tv_fingerprint_name);
            btnDelete = (Button) itemView.findViewById(R.id.btn_delete_fingerprint);
        }
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.layout_item_fringerprint,null);
        MyViewHolder myViewHolder = new MyViewHolder(view);
        return myViewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {
        final Fingerprint fingerprint = fingerprintList.get(position);
        holder.tvFingerprintName.setText(
                String.format("%s",fingerprint.getName())
        );
        holder.btnDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (callBack != null){
                    callBack.remove(fingerprint);
                }
            }
        });

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (callBack != null){
                    callBack.rename(fingerprint);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return fingerprintList.size();
    }

    public void setFingerprintList(List<Fingerprint> fingerprintList) {
        this.fingerprintList = fingerprintList;
    }

    public void setCallBack(ClickCallBack callBack) {
        this.callBack = callBack;
    }

}
