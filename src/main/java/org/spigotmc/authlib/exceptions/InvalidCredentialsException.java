package org.spigotmc.authlib.exceptions;

import org.spigotmc.authlib.exceptions.AuthenticationException;

public class InvalidCredentialsException extends AuthenticationException
{
    public InvalidCredentialsException() {
    }

    public InvalidCredentialsException(String message) {
        super(message);
    }

    public InvalidCredentialsException(String message, Throwable cause) {
        super(message, cause);
    }

    public InvalidCredentialsException(Throwable cause) {
        super(cause);
    }
}
