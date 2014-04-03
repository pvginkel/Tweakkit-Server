package org.spigotmc.authlib.yggdrasil.request;

import org.spigotmc.authlib.GameProfile;
import org.spigotmc.authlib.yggdrasil.YggdrasilUserAuthentication;

public class RefreshRequest {
    private String clientToken;
    private String accessToken;
    private GameProfile selectedProfile;
    private boolean requestUser = true;

    public RefreshRequest(YggdrasilUserAuthentication authenticationService) {
        this(authenticationService, null);
    }

    public RefreshRequest(YggdrasilUserAuthentication authenticationService, GameProfile profile) {
        this.clientToken = authenticationService.getAuthenticationService().getClientToken();
        this.accessToken = authenticationService.getAuthenticatedToken();
        this.selectedProfile = profile;
    }
}
