package org.spigotmc.authlib.minecraft;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class MinecraftProfileTexture {
    public enum Type {
        SKIN,
        CAPE,
        ;
    }

    private final String url;

    public MinecraftProfileTexture(String url) {
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public String getHash() {
        return FilenameUtils.getBaseName(url);
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("url", url)
                .append("hash", getHash())
                .toString();
    }
}
