package org.spigotmc.authlib.yggdrasil.request;

import org.spigotmc.authlib.yggdrasil.YggdrasilUserAuthentication;

public class InvalidateRequest {
    private String accessToken;
    private String clientToken;

    public InvalidateRequest(YggdrasilUserAuthentication authenticationService) {
        this.accessToken = authenticationService.getAuthenticatedToken();
        this.clientToken = authenticationService.getAuthenticationService().getClientToken();
    }
}
