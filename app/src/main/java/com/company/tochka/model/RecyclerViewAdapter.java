package com.company.tochka.model;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;


import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.company.tochka.R;
import com.company.tochka.databinding.FragmentUserBinding;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder>{

    private static final int ITEM = 0;
    private static final int LOADING = 1;

    private boolean isLoadingAdded;

    private ArrayList<User> arrayList = new ArrayList<>();

    private RecyclerViewAdapterCallback mCallback;

    public RecyclerViewAdapter(Context context){
        this.mCallback = (RecyclerViewAdapterCallback) context;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        RecyclerView.ViewHolder viewHolder;
        LayoutInflater layoutInflater = LayoutInflater.from(parent.getContext());

        switch (viewType){

            case ITEM:

                FragmentUserBinding itemBinding =
                        FragmentUserBinding.inflate(layoutInflater, parent, false);

                viewHolder = new ItemViewHolder(itemBinding);
                return viewHolder;


            case LOADING:

                View viewLoading = layoutInflater.inflate(R.layout.fragment_loading, parent, false);
                viewHolder = new LoadingViewHolder(viewLoading);

                return viewHolder;
        }

        return null;
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {

        if(getItemViewType(position) == ITEM){

            User currentUser = arrayList.get(position);
            currentUser.setNumberInList(Integer.toString(position + 1));

            if(position == arrayList.size() - 1)
                currentUser.setLastInList(true);

            ((ItemViewHolder) holder).bind(currentUser);
        }
    }
    
    public void addAll(ArrayList<User> arrayList){

        for (User user : arrayList) {
            this.arrayList.add(user);
            notifyItemChanged(arrayList.size() - 1);
        }
    }

    public void addLoadingFooter() {
        isLoadingAdded = true;
        arrayList.add(new User());
        notifyItemInserted(arrayList.size() - 1);
    }

    public void removeLoadingFooter() {
        if(!isLoadingAdded)
            return;

        isLoadingAdded = false;

        int position = arrayList.size() - 1;
        User user = arrayList.get(position);

        if (user != null) {
            arrayList.remove(position);
            notifyItemRemoved(position);
        }
    }

    public void removeAll(){
        removeLoadingFooter();
        arrayList.clear();
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return arrayList == null ? 0 : arrayList.size();
    }

    @Override
    public int getItemViewType(int position) {

        if(position == arrayList.size() - 1 && isLoadingAdded)
            return LOADING;
         else
            return ITEM;

    }

    private class ItemViewHolder extends RecyclerView.ViewHolder{

        private final FragmentUserBinding binding;

        ItemViewHolder(FragmentUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(User user){
            binding.setUser(user);
            binding.view.setOnClickListener(v -> mCallback.openFullUserInformation(user.getLogin()));

            binding.executePendingBindings();
        }
    }

    public boolean isLoadingAdded() {
        return isLoadingAdded;
    }

    public void setLoadingAdded(boolean isLoadingAdded){
        this.isLoadingAdded = isLoadingAdded;
    }

    public ArrayList<User> getArrayList(){
        return arrayList;
    }

    private class LoadingViewHolder extends RecyclerView.ViewHolder{

        LoadingViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    public interface RecyclerViewAdapterCallback {
        void openFullUserInformation(String login);
    }
}