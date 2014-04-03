package org.spigotmc.authlib.minecraft;

import org.spigotmc.authlib.GameProfile;
import org.spigotmc.authlib.exceptions.AuthenticationException;
import org.spigotmc.authlib.exceptions.AuthenticationUnavailableException;
import org.spigotmc.authlib.minecraft.MinecraftProfileTexture;

import java.util.Map;

public interface MinecraftSessionService {
    /**
     * Attempts to join the specified Minecraft server.
     * <p />
     * The {@link org.spigotmc.authlib.GameProfile} used to join with may be partial, but the exact requirements will vary on
     * authentication service. If this method returns without throwing an exception, the join was successful and a subsequent call to
     * {@link #hasJoinedServer(org.spigotmc.authlib.GameProfile, String)} will return true.
     *
     * @param profile Partial {@link org.spigotmc.authlib.GameProfile} to join as
     * @param authenticationToken The {@link org.spigotmc.authlib.UserAuthentication#getAuthenticatedToken() authenticated token} of the user
     * @param serverId The random ID of the server to join
     * @throws org.spigotmc.authlib.exceptions.AuthenticationUnavailableException Thrown when the servers return a malformed response, or are otherwise unavailable
     * @throws org.spigotmc.authlib.exceptions.InvalidCredentialsException Thrown when the specified authenticationToken is invalid
     * @throws org.spigotmc.authlib.exceptions.AuthenticationException Generic exception indicating that we could not authenticate the user
     */
    public void joinServer(GameProfile profile, String authenticationToken, String serverId) throws AuthenticationException;

    /**
     * Checks if the specified user has joined a Minecraft server.
     * <p />
     * The {@link org.spigotmc.authlib.GameProfile} used to join with may be partial, but the exact requirements will vary on
     * authentication service.
     *
     * @param user Partial {@link org.spigotmc.authlib.GameProfile} to check for
     * @param serverId The random ID of the server to check for
     * @throws org.spigotmc.authlib.exceptions.AuthenticationUnavailableException Thrown when the servers return a malformed response, or are otherwise unavailable
     * @return Full game profile if the user had joined, otherwise null
     */
    public GameProfile hasJoinedServer(GameProfile user, String serverId) throws AuthenticationUnavailableException;

    /**
     * Gets a map of all known textures from a {@link org.spigotmc.authlib.GameProfile}.
     * <p />
     * If a profile contains invalid textures, they will not be returned. If a profile contains no textures, an empty map will be returned.
     *
     * @param profile Game profile to return textures from.
     * @param requireSecure If true, requires the payload to be recent and securely fetched.
     * @return Map of texture types to textures.
     */
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile profile, boolean requireSecure);

    /**
     * Fills a profile with all known properties from the session service.
     * <p />
     * The profile must have an ID. If no information is found, nothing will be done.
     *
     * @param profile Game profile to fill with properties.
     * @return Filled profile for the previous user.
     */
    public GameProfile fillProfileProperties(GameProfile profile);
}
