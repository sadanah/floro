package com.sadanah.floro;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class SearchFragment extends Fragment {

    private List<Disease> allDiseases;
    private List<Disease> filteredDiseases;
    private DiseaseAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_search, container, false);

        SearchView searchView = root.findViewById(R.id.search_view);
        RecyclerView recyclerView = root.findViewById(R.id.recycler_diseases);

        // Hardcoded disease list
        allDiseases = new ArrayList<>();
        allDiseases.add(new Disease("Anthracnose", "Fungal disease"));
        allDiseases.add(new Disease("Soft Rot", "Bacterial disease"));

        filteredDiseases = new ArrayList<>(allDiseases);

        // Adapter with click -> ResultFragment
        adapter = new DiseaseAdapter(filteredDiseases, disease -> {
            ResultFragment frag = ResultFragment.newInstance(disease.getName(), 0f);
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragment_container, frag)
                    .addToBackStack("result")
                    .commit();
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);

        // Search filter
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) { return false; }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterDiseases(newText);
                return true;
            }
        });

        return root;
    }

    private void filterDiseases(String query) {
        filteredDiseases.clear();
        for (Disease d : allDiseases) {
            if (d.getName().toLowerCase().contains(query.toLowerCase())) {
                filteredDiseases.add(d);
            }
        }
        adapter.notifyDataSetChanged();
    }
}
