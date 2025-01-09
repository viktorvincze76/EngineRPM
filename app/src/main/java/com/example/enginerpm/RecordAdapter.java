package com.example.enginerpm;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class RecordAdapter extends RecyclerView.Adapter<RecordAdapter.RecordViewHolder> {

    public interface OnRecordClickListener {
        void onRecordClick(String recordName);
    }

    private final List<String> records;
    private final List<String> selectedRecords;
    private final OnRecordClickListener listener;

    public RecordAdapter(List<String> records, OnRecordClickListener listener) {
        this.records = records;
        this.listener = listener;
        this.selectedRecords = new ArrayList<>();
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        String record = records.get(position);
        holder.tvRecordName.setText(record);

        // Set click listener for the item
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecordClick(record);
            }
        });

        // Checkbox listener for selecting records
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                selectedRecords.add(record);
            } else {
                selectedRecords.remove(record);
            }
        });

        // Reset checkbox state
        holder.checkBox.setChecked(selectedRecords.contains(record));
    }

    @Override
    public int getItemCount() {
        return records.size();
    }

    public void addRecord(String record) {
        records.add(record);
        notifyDataSetChanged();
    }

    public void removeRecords(List<String> recordsToRemove) {
        records.removeAll(recordsToRemove);
        notifyDataSetChanged();
    }

    public ArrayList<String> getSelectedRecords() {
        return new ArrayList<>(selectedRecords);
    }

    public static class RecordViewHolder extends RecyclerView.ViewHolder {
        TextView tvRecordName;
        CheckBox checkBox;

        public RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            tvRecordName = itemView.findViewById(R.id.tv_record_name);
            checkBox = itemView.findViewById(R.id.checkbox_record);
        }
    }

    public void addAll(List<String> newRecords) {
        records.addAll(newRecords);
        notifyDataSetChanged();
    }


}
