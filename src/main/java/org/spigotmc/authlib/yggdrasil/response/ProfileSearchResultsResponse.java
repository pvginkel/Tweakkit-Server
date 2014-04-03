package org.spigotmc.authlib.yggdrasil.response;

import org.spigotmc.authlib.GameProfile;
import org.spigotmc.authlib.yggdrasil.response.Response;

public class ProfileSearchResultsResponse extends Response
{
    private GameProfile[] profiles;
    private int size;

    public GameProfile[] getProfiles() {
        return profiles;
    }

    public int getSize() {
        return size;
    }
}
