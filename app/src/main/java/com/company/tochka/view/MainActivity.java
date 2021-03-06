package com.company.tochka.view;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.core.view.ViewCompat;
import androidx.databinding.BindingAdapter;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ActivityOptions;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.company.tochka.view_model.MyViewModel;
import com.company.tochka.R;
import com.squareup.picasso.Picasso;

import java.util.Objects;

import static com.company.tochka.model.Exceptions.LOAD_FIRST_PAGE_EXCEPTION;
import static com.company.tochka.model.Exceptions.LOAD_NEXT_PAGE_EXCEPTION;
import static com.company.tochka.model.Exceptions.OPEN_FULL_USER_INFORMATION_EXCEPTION;
import static com.company.tochka.model.Exceptions.SEARCH_EXCEPTION;
import static com.company.tochka.model.Exceptions.SEARCH_NEXT_PAGE_EXCEPTION;

public class MainActivity extends AppCompatActivity implements RecyclerViewAdapter.RecyclerViewAdapterCallback {

    private RecyclerViewAdapter adapter;
    private LinearLayoutManager linearLayoutManager;
    private ProgressBar progressBar;
    private CustomAlertDialog customAlertDialogInfo;

    MyViewModel model;

    private boolean isLoading = false;
    private boolean isLastPage = false;
    private boolean isSearch = false;

    private ImageView sharedImageView;
    private CardView sharedCardView;
    private TextView sharedTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        adapter = new RecyclerViewAdapter(this);

        linearLayoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);

        progressBar = findViewById(R.id.progressBar);

        Toolbar toolbar = findViewById(R.id.tool);
        toolbar.setTitleTextColor(getColor(R.color.colorWhite));
        toolbar.setTitle(R.string.app_name);
        setSupportActionBar(toolbar);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(linearLayoutManager);
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(adapter);

        model = new ViewModelProvider(this).get(MyViewModel.class);

        model.subscribeCurrentRecyclerStatus().observe(this, newRecyclerViewStatus -> {
            isLoading = newRecyclerViewStatus.isLoading();
            isLastPage = newRecyclerViewStatus.isLastPage();
            isSearch = newRecyclerViewStatus.isSearch();
        });

        model.subscribeCurrentArrayList().observe(this, users -> {
            progressBar.setVisibility(View.INVISIBLE);

            adapter.removeAll();

            if(users.size() == 0){
                Toast.makeText(this, R.string.nothing_was_found_for_your_search, Toast.LENGTH_SHORT).show();
                return;
            }

            adapter.addAll(users);

            if (!isLastPage){
                adapter.addLoadingFooter();
            }
        });

        model.subscribeFullUser().observe(this, fullUser -> {

            if (sharedTextView == null || sharedImageView == null) {
                return;
            }

            Intent  fullUserIntent = new Intent(this, UserActivity.class);

            fullUserIntent.putExtra("user", fullUser);
            fullUserIntent.putExtra("sharedImageView", ViewCompat.getTransitionName(sharedImageView));
            fullUserIntent.putExtra("sharedCardView", ViewCompat.getTransitionName(sharedCardView));
            fullUserIntent.putExtra("sharedTextView", ViewCompat.getTransitionName(sharedTextView));

            Pair<View,String> imageViewPair = new Pair<>(sharedImageView,ViewCompat.getTransitionName(sharedImageView));
            Pair<View,String> cardViewPair = new Pair<>(sharedCardView,ViewCompat.getTransitionName(sharedCardView));
            Pair<View,String> textViewPair = new Pair<>(sharedTextView,ViewCompat.getTransitionName(sharedTextView));

            ActivityOptions options = ActivityOptions.makeSceneTransitionAnimation(this,
                    imageViewPair,
                    cardViewPair,
                    textViewPair);

            progressBar.setVisibility(View.INVISIBLE);

            startActivity(fullUserIntent, options.toBundle());
        });

        model.subscribeOnExceptions().observe(this, exception -> {
            switch (exception){

                case LOAD_FIRST_PAGE_EXCEPTION:
                    showAlertDialog(v -> {
                        model.loadFirstPageAfterException();
                        customAlertDialogInfo.hide();
                    });

                    break;

                case LOAD_NEXT_PAGE_EXCEPTION:
                    showAlertDialog(v -> {
                        model.loadNextPageAfterException();
                        customAlertDialogInfo.hide();
                    });

                    break;

                case SEARCH_EXCEPTION:
                    showAlertDialog(v -> {
                        model.searchAfterException();
                        customAlertDialogInfo.hide();
                    });

                    break;

                case SEARCH_NEXT_PAGE_EXCEPTION:
                    showAlertDialog(v -> {
                        model.searchNextPageAfterException();
                        customAlertDialogInfo.hide();
                    });

                    break;

                case OPEN_FULL_USER_INFORMATION_EXCEPTION:
                    showAlertDialog(v -> {
                        model.searchFullUserInformationAfterException();
                        customAlertDialogInfo.hide();
                    });

                    break;
            }
        });

        model.getData();

        recyclerView.addOnScrollListener(new PaginationScrollListener(linearLayoutManager) {
            @Override
            protected void loadMoreItems() {

                model.setIsLoading(true);

                if (isSearch)
                    model.searchNextPage();
                else
                    model.loadNextPage();
            }

            @Override
            public boolean isLastPage() {
                return isLastPage;
            }

            @Override
            public boolean isLoading() {
                return isLoading;
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.main_activity_menu, menu);

        MenuItem searchItem = menu.findItem(R.id.action_search);

        SearchManager searchManager = (SearchManager) getSystemService(Context.SEARCH_SERVICE);
        SearchView searchView = (SearchView) menu.findItem(R.id.action_search).getActionView();

        searchView.setSearchableInfo(Objects.requireNonNull(searchManager).getSearchableInfo(getComponentName()));

        if(isSearch){
            searchView.onActionViewExpanded();
            searchView.setQuery(model.getCurrentUserName(), true);
            searchView.clearFocus();
        }

        ImageView closeButton = searchView.findViewById(R.id.search_close_btn);

        closeButton.setOnClickListener(v -> {

            if (isSearch) {

                progressBar.setVisibility(View.VISIBLE);

                adapter.removeAll();
                model.loadFirstPage();
            }

            searchView.setQuery("", false);
            searchView.onActionViewCollapsed();
            searchItem.collapseActionView();
        });

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String query) {

                progressBar.setVisibility(View.VISIBLE);

                adapter.removeAll();

                model.search(query);

                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    protected void onPause() {

        if(customAlertDialogInfo != null)
            customAlertDialogInfo.dismiss();

        super.onPause();
    }

    private void showAlertDialog(View.OnClickListener onClickListener){

        if (customAlertDialogInfo == null)
            customAlertDialogInfo = new CustomAlertDialog(MainActivity.this,
                    R.string.alert_dialog_error);

        customAlertDialogInfo.setTitle(R.string.something_went_wrong);
        customAlertDialogInfo.setMessage(R.string.please_try_again);
        customAlertDialogInfo.show();

        customAlertDialogInfo.setButtonClickListener(onClickListener);
    }

    @Override
    public void openFullUserInformation(String userLogin, ImageView sharedImageView, CardView sharedCardView, TextView sharedTextView) {
        this.sharedImageView = sharedImageView;
        this.sharedCardView = sharedCardView;
        this.sharedTextView = sharedTextView;

        progressBar.setVisibility(View.VISIBLE);
        model.searchFullUserInformation(userLogin);
    }

    @BindingAdapter({"url"})
    public static void loadImage(ImageView view, String url) {
        Picasso.get().load(url).into(view);
    }
}