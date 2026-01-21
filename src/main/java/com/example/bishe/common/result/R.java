package com.example.bishe.common.result;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class R<T> {
    private int code;
    private String msg;
    private T data;

    public static <T> R<T> success(T data) {
        return new R<>(200, "ok", data);
    }

    public static <T> R<T> failed(String msg) {
        return new R<>(400, msg, null);
    }
}