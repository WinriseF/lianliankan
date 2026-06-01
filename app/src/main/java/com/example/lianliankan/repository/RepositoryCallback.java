package com.example.lianliankan.repository;

public interface RepositoryCallback<T> {
    void onSuccess(T value);

    void onError(Exception error);
}
