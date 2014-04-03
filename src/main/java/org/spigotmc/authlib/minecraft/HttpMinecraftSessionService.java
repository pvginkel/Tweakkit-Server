package org.spigotmc.authlib.minecraft;

import org.spigotmc.authlib.HttpAuthenticationService;
import org.spigotmc.authlib.minecraft.BaseMinecraftSessionService;

public abstract class HttpMinecraftSessionService extends BaseMinecraftSessionService
{
    protected HttpMinecraftSessionService(HttpAuthenticationService authenticationService) {
        super(authenticationService);
    }

    @Override
    public HttpAuthenticationService getAuthenticationService() {
        return (HttpAuthenticationService) super.getAuthenticationService();
    }
}
