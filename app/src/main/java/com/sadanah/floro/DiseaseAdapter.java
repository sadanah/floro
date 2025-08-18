package com.sadanah.floro;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class DiseaseAdapter extends RecyclerView.Adapter<DiseaseAdapter.ViewHolder> {

    public interface OnDiseaseClickListener {
        void onDiseaseClick(Disease disease);
    }

    private List<Disease> diseaseList;
    private OnDiseaseClickListener listener;

    public DiseaseAdapter(List<Disease> diseaseList, OnDiseaseClickListener listener) {
        this.diseaseList = diseaseList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.disease_item, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Disease disease = diseaseList.get(position);
        holder.tvName.setText(disease.getName());
        holder.tvType.setText(disease.getType());

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onDiseaseClick(disease);
        });
    }

    @Override
    public int getItemCount() {
        return diseaseList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvType;
        ViewHolder(View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_disease_name);
            tvType = itemView.findViewById(R.id.tv_disease_type);
        }
    }
}

