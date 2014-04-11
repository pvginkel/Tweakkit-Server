package org.spigotmc.authlib.legacy;

import org.spigotmc.authlib.GameProfile;
import org.spigotmc.authlib.exceptions.AuthenticationException;
import org.spigotmc.authlib.exceptions.AuthenticationUnavailableException;
import org.spigotmc.authlib.minecraft.HttpMinecraftSessionService;
import org.spigotmc.authlib.minecraft.MinecraftProfileTexture;
import org.spigotmc.authlib.legacy.LegacyAuthenticationService;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import static org.spigotmc.authlib.HttpAuthenticationService.*;

public class LegacyMinecraftSessionService extends HttpMinecraftSessionService {
    private static final String BASE_URL = "http://session.minecraft.net/game/";
    private static final URL JOIN_URL = constantURL(BASE_URL + "joinserver.jsp");
    private static final URL CHECK_URL = constantURL(BASE_URL + "checkserver.jsp");

    protected LegacyMinecraftSessionService(LegacyAuthenticationService authenticationService) {
        super(authenticationService);
    }

    @Override
    public void joinServer(GameProfile profile, String authenticationToken, String serverId) throws AuthenticationException {
        Map<String, Object> arguments = new HashMap<String, Object>();

        arguments.put("user", profile.getName());
        arguments.put("sessionId", authenticationToken);
        arguments.put("serverId", serverId);

        URL url = concatenateURL(JOIN_URL, buildQuery(arguments));

        try {
            String response = getAuthenticationService().performGetRequest(url);

            if (!response.equals("OK")) {
                throw new AuthenticationException(response);
            }
        } catch (IOException e) {
            throw new AuthenticationUnavailableException(e);
        }
    }

    @Override
    public GameProfile hasJoinedServer(GameProfile user, String serverId) throws AuthenticationUnavailableException {
        Map<String, Object> arguments = new HashMap<String, Object>();

        arguments.put("user", user.getName());
        arguments.put("serverId", serverId);

        URL url = concatenateURL(CHECK_URL, buildQuery(arguments));

        try {
            String response = getAuthenticationService().performGetRequest(url);

            return response.equals("YES") ? user : null;
        } catch (IOException e) {
            throw new AuthenticationUnavailableException(e);
        }
    }

    @Override
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile profile, boolean requireSecure) {
        return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
    }

    @Override
    public GameProfile fillProfileProperties(GameProfile profile) {
        return profile;
    }

    @Override
    public LegacyAuthenticationService getAuthenticationService() {
        return (LegacyAuthenticationService) super.getAuthenticationService();
    }
}
