package org.spigotmc.authlib.yggdrasil.response;

import org.spigotmc.authlib.properties.PropertyMap;
import org.spigotmc.authlib.yggdrasil.response.Response;

import java.util.UUID;

public class MinecraftProfilePropertiesResponse extends Response
{
    private UUID id;
    private String name;
    private PropertyMap properties;

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public PropertyMap getProperties() {
        return properties;
    }
}
