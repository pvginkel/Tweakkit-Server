package org.spigotmc.authlib.yggdrasil.response;

import org.spigotmc.authlib.GameProfile;
import org.spigotmc.authlib.yggdrasil.response.Response;
import org.spigotmc.authlib.yggdrasil.response.User;

public class RefreshResponse extends Response
{
    private String accessToken;
    private String clientToken;
    private GameProfile selectedProfile;
    private GameProfile[] availableProfiles;
    private User user;

    public String getAccessToken() {
        return accessToken;
    }

    public String getClientToken() {
        return clientToken;
    }

    public GameProfile[] getAvailableProfiles() {
        return availableProfiles;
    }

    public GameProfile getSelectedProfile() {
        return selectedProfile;
    }

    public User getUser() {
        return user;
    }
}
