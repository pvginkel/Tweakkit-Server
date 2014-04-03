package org.spigotmc.authlib;

public interface GameProfileRepository {
    public void findProfilesByNames(String[] names, Agent agent, ProfileLookupCallback callback);
}
