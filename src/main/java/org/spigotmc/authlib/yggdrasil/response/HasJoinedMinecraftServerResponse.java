package org.spigotmc.authlib.yggdrasil.response;

import org.spigotmc.authlib.properties.PropertyMap;
import org.spigotmc.authlib.yggdrasil.response.Response;

import java.util.UUID;

public class HasJoinedMinecraftServerResponse extends Response
{
    private UUID id;
    private PropertyMap properties;

    public UUID getId() {
        return id;
    }

    public PropertyMap getProperties() {
        return properties;
    }
}
