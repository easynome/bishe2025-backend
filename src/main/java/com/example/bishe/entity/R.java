package com.example.bishe.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class R<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> R<T> success(T data) {
        return new R<>(200, "ok", data);
    }
}