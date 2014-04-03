package org.spigotmc.authlib.yggdrasil.response;

public class Response {
    private String error;
    private String errorMessage;
    private String cause;

    public String getError() {
        return error;
    }

    public String getCause() {
        return cause;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
