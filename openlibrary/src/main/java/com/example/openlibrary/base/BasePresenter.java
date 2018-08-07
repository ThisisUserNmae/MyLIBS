package com.example.openlibrary.base;

import io.reactivex.disposables.CompositeDisposable;

public abstract class BasePresenter<V extends IView> {
    protected V view;
    protected CompositeDisposable compositeDisposable = new CompositeDisposable();

    public BasePresenter(V view) {
        this.view = view;
        initModel();
    }

    protected abstract void initModel();

    void onDestroy() {
        view = null;
        compositeDisposable.clear();
    }
}
