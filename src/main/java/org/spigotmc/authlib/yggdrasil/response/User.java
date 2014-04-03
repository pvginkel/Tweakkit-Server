package org.spigotmc.authlib.yggdrasil.response;

import org.spigotmc.authlib.properties.PropertyMap;

public class User {
    private String id;
    private PropertyMap properties;

    public String getId() {
        return id;
    }

    public PropertyMap getProperties() {
        return properties;
    }
}
