package net.kdt.pojavlaunch.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.squareup.picasso.Picasso;
import net.kdt.pojavlaunch.R;
import net.kdt.pojavlaunch.modmanager.ModManager;
import net.kdt.pojavlaunch.modmanager.State;
import net.kdt.pojavlaunch.modmanager.api.Curseforge;
import net.kdt.pojavlaunch.modmanager.ModData;
import net.kdt.pojavlaunch.modmanager.api.Fabric;
import net.kdt.pojavlaunch.modmanager.api.Modrinth;
import us.feras.mdv.MarkdownView;

import java.util.ArrayList;

public class ModsFragment extends Fragment {

    private String filter = "Modrinth";

    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);
        View view = inflater.inflate(R.layout.fragment_mods, container, false);

        ModAdapter modAdapter = new ModAdapter(this);
        RecyclerView modRecycler = view.findViewById(R.id.mods_recycler);
        modRecycler.setLayoutManager(new LinearLayoutManager(modRecycler.getContext()));
        modRecycler.setAdapter(modAdapter);
        modAdapter.setFilter(filter);

        String[] filters = new String[] {"Modrinth", "CurseForge", "Installed", "Core"};
        ArrayAdapter<String> filterAdapter = new ArrayAdapter<>(this.getActivity(), android.R.layout.simple_spinner_item, filters);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_list_item_single_choice);
        Spinner filterSpinner = view.findViewById(R.id.filter_spinner);
        filterSpinner.setAdapter(filterAdapter);

        SearchView modSearch = view.findViewById(R.id.mods_search);
        modSearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                loadDataIntoList(modAdapter, s, 0, true);
                return true;
            }
        });

        modRecycler.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollVertically(1)) {
                    modAdapter.setFilter(filter);
                    loadDataIntoList(modAdapter, "", modAdapter.getOffset(), false);
                }
            }
        });

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                filter = filters[i];
                loadDataIntoList(modAdapter, "", 0, true);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });

        return view;
    }

    private void loadDataIntoList(ModAdapter modAdapter, String query, int offset, boolean refresh) {
        if (refresh) modAdapter.reset();
        State.Instance selectedInstance = ModManager.state.getInstance("fabric-loader-" + Fabric.getLatestLoaderVersion() + "-1.18.2");

        if (filter.equals("Modrinth")) Modrinth.addProjectsToRecycler(modAdapter, selectedInstance.getGameVersion(), offset, query);
        else if (filter.equals("CurseForge")) Curseforge.addProjectsToRecycler(modAdapter, selectedInstance.getGameVersion(), offset, query);
        else if (filter.equals("Installed")) {
            ArrayList<ModData> mods = ModManager.listInstalledMods(selectedInstance.getName());
            if (mods.size() != 0) modAdapter.addMods(mods);
        }
        else if (filter.equals("Core")) {
            ArrayList<ModData> mods = ModManager.listCoreMods(selectedInstance.getGameVersion());
            if (mods.size() != 0) modAdapter.addMods(mods);
        }
    }

    public static class ModViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        private final ModsFragment fragment;
        private final ImageView icon;
        private final TextView title;
        private final TextView compat;
        private final Switch enableSwitch;
        private ModData modData;
        private String filter;

        public ModViewHolder(View view, ModsFragment fragment) {
            super(view);
            this.fragment = fragment;
            view.setOnClickListener(this);
            icon = view.findViewById(R.id.mod_icon);
            title = view.findViewById(R.id.mod_title);
            compat = view.findViewById(R.id.mod_details);
            enableSwitch = view.findViewById(R.id.mod_switch);

            enableSwitch.setOnCheckedChangeListener((button, value) -> {
                if (ModManager.isDownloading(modData.slug)) {
                    button.setChecked(true);
                    return;
                }

                State.Instance selectedInstance = ModManager.state.getInstance("fabric-loader-" + Fabric.getLatestLoaderVersion() + "-1.18.2");
                ModData mod = ModManager.getMod(selectedInstance.getName(), modData.slug);
                if (mod != null) ModManager.setModActive(selectedInstance.getName(), this.modData.slug, value);
                else ModManager.addMod(selectedInstance.getName(), filter.toLowerCase(), this.modData.slug, selectedInstance.getGameVersion(), false);
            });
        }

        public void setData(ModData modData) {
            ModData installedMod = ModManager.getMod("fabric-loader-" + Fabric.getLatestLoaderVersion() + "-1.18.2", modData.slug);
            if (installedMod != null) modData = installedMod; //Check if mod in already installed and overwrite fetched data

            this.modData = modData;
            title.setText(modData.title);
            if (!modData.iconUrl.isEmpty()) Picasso.get().load(modData.iconUrl).into(icon);

            String modCompat = ModManager.getModCompat(modData.slug);
            compat.setText("  " + modCompat + "  ");
            if (modCompat.equals("Untested")) compat.setBackgroundResource(R.drawable.marker_gray);
            if (modCompat.equals("Perfect")) compat.setBackgroundResource(R.drawable.marker_green);
            if (modCompat.equals("Good")) compat.setBackgroundResource(R.drawable.marker_yellow);
            if (modCompat.equals("Unusable") || modCompat.equals("Not Working")) compat.setBackgroundResource(R.drawable.marker_red);

            enableSwitch.setChecked(modData.isActive);
        }

        public void setFilter(String filter) {
            this.filter = filter;
        }

        @Override
        public void onClick(View view) {
            View fView = fragment.getView();
            if (fView != null) {
                ImageView iconMain = fView.findViewById(R.id.mod_icon_main);
                TextView titleMain = fView.findViewById(R.id.mod_title_main);
                MarkdownView bodyMain = fView.findViewById(R.id.mod_description);
                iconMain.setImageDrawable(icon.getDrawable());
                titleMain.setText(modData.title);
                if (filter.equals("Modrinth")) Modrinth.loadProjectPage(bodyMain, modData.slug);
                if (filter.equals("Curseforge")) Curseforge.loadProjectPage(bodyMain, modData.slug);
            }
        }
    }

    public static class ModAdapter extends RecyclerView.Adapter<ModViewHolder> {

        private final ModsFragment fragment;
        private final ArrayList<ModData> mods = new ArrayList<>();
        private String filter;
        //private int lastPosition = -1;

        public ModAdapter(ModsFragment fragment) {
            this.fragment = fragment;
        }

        public void addMods(ArrayList<ModData> newMods) {
            int startPos = mods.size();
            mods.addAll(newMods);
            this.notifyItemRangeChanged(startPos, mods.size());
        }

        public void reset() {
            mods.clear();
            this.notifyDataSetChanged();
        }

        //Needs testing - might need + or - 1
        public int getOffset() {
            return mods.size();
        }

        public void setFilter(String filter) {
            this.filter = filter;
        }

        @Override
        public int getItemViewType(final int position) {
            return R.layout.item_mod;
        }

        @NonNull
        @Override
        public ModViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(viewType, parent, false);
            return new ModViewHolder(view, fragment);
        }

        @Override
        public void onBindViewHolder(@NonNull ModViewHolder holder, int position) {
            if (mods.size() > position) {
                holder.setData(mods.get(position));
                holder.setFilter(filter);
                //setAnimation(holder.itemView, position);
            }
        }

        /*private void setAnimation(View viewToAnimate, int position) {
            if (position > lastPosition) {
                Animation animation = AnimationUtils.loadAnimation(fragment.getContext(), android.R.anim.slide_in_left);
                viewToAnimate.startAnimation(animation);
                lastPosition = position;
            }
        }*/

        @Override
        public int getItemCount() {
            return mods.size();
        }
    }
}