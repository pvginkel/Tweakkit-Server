package org.spigotmc.authlib.minecraft;

import org.spigotmc.authlib.AuthenticationService;
import org.spigotmc.authlib.minecraft.MinecraftSessionService;

public abstract class BaseMinecraftSessionService implements MinecraftSessionService
{
    private final AuthenticationService authenticationService;

    protected BaseMinecraftSessionService(AuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    public AuthenticationService getAuthenticationService() {
        return authenticationService;
    }
}
