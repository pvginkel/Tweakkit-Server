package org.spigotmc.authlib.yggdrasil.request;

import org.spigotmc.authlib.Agent;
import org.spigotmc.authlib.yggdrasil.YggdrasilUserAuthentication;

public class AuthenticationRequest {
    private Agent agent;
    private String username;
    private String password;
    private String clientToken;
    private boolean requestUser = true;

    public AuthenticationRequest(YggdrasilUserAuthentication authenticationService, String username, String password) {
        this.agent = authenticationService.getAgent();
        this.username = username;
        this.clientToken = authenticationService.getAuthenticationService().getClientToken();
        this.password = password;
    }
}
