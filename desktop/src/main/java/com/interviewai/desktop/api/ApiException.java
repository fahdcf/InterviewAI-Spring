package com.interviewai.desktop.api;

import java.io.IOException;

public class ApiException extends IOException {

    private final int status;

    public ApiException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
