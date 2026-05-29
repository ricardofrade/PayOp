package com.rfrade.payop.data.terminal;

import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;

public final class LiveDataAdapter {

    private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

    private LiveDataAdapter() {

    }

    public static <T> Single<T> toSingle(LiveData<T> liveData) {
        return Single.<T>create(
                        emitter -> {
                            final Observer<T> observer =
                                    new Observer<T>() {
                                        @Override
                                        public void onChanged(T value) {
                                            if (value != null && !emitter.isDisposed()) {
                                                liveData.removeObserver(this);
                                                emitter.onSuccess(value);
                                            }
                                        }
                                    };

                            MAIN_HANDLER.post(
                                    () -> {
                                        if (!emitter.isDisposed()) {
                                            liveData.observeForever(observer);
                                        }
                                    });

                            emitter.setCancellable(
                                    () ->
                                            MAIN_HANDLER.post(
                                                    () -> liveData.removeObserver(observer)));
                        })
                .observeOn(Schedulers.io());
    }
}
