package org.spigotmc.authlib.yggdrasil;

import com.google.common.collect.Iterables;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.minecraft.util.org.apache.commons.io.Charsets;
import org.spigotmc.authlib.GameProfile;
import org.spigotmc.authlib.HttpAuthenticationService;
import org.spigotmc.authlib.exceptions.AuthenticationException;
import org.spigotmc.authlib.exceptions.AuthenticationUnavailableException;
import org.spigotmc.authlib.minecraft.HttpMinecraftSessionService;
import org.spigotmc.authlib.minecraft.MinecraftProfileTexture;
import org.spigotmc.authlib.properties.Property;
import org.spigotmc.authlib.yggdrasil.YggdrasilAuthenticationService;
import org.spigotmc.authlib.yggdrasil.request.JoinMinecraftServerRequest;
import org.spigotmc.authlib.yggdrasil.response.HasJoinedMinecraftServerResponse;
import org.spigotmc.authlib.yggdrasil.response.MinecraftProfilePropertiesResponse;
import org.spigotmc.authlib.yggdrasil.response.MinecraftTexturesPayload;
import org.spigotmc.authlib.yggdrasil.response.Response;
import org.spigotmc.authlib.util.UUIDTypeAdapter;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.net.URL;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.*;

public class YggdrasilMinecraftSessionService extends HttpMinecraftSessionService {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String BASE_URL = "https://sessionserver.mojang.com/session/minecraft/";
    private static final URL JOIN_URL = HttpAuthenticationService.constantURL(BASE_URL + "join");
    private static final URL CHECK_URL = HttpAuthenticationService.constantURL(BASE_URL + "hasJoined");

    private final PublicKey publicKey;
    private final Gson gson = new GsonBuilder().registerTypeAdapter(UUID.class, new UUIDTypeAdapter()).create();

    protected YggdrasilMinecraftSessionService(YggdrasilAuthenticationService authenticationService) {
        super(authenticationService);

        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(IOUtils.toByteArray(YggdrasilMinecraftSessionService.class.getResourceAsStream("/yggdrasil_session_pubkey.der")));
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            publicKey = keyFactory.generatePublic(spec);
        } catch (Exception e) {
            throw new Error("Missing/invalid yggdrasil public key!");
        }
    }

    @Override
    public void joinServer(GameProfile profile, String authenticationToken, String serverId) throws AuthenticationException {
        JoinMinecraftServerRequest request = new JoinMinecraftServerRequest();
        request.accessToken = authenticationToken;
        request.selectedProfile = profile.getId();
        request.serverId = serverId;

        getAuthenticationService().makeRequest(JOIN_URL, request, Response.class);
    }

    @Override
    public GameProfile hasJoinedServer(GameProfile user, String serverId) throws AuthenticationUnavailableException {
        Map<String, Object> arguments = new HashMap<String, Object>();

        arguments.put("username", user.getName());
        arguments.put("serverId", serverId);

        URL url = HttpAuthenticationService.concatenateURL(CHECK_URL, HttpAuthenticationService.buildQuery(arguments));

        try {
            HasJoinedMinecraftServerResponse response = getAuthenticationService().makeRequest(url, null, HasJoinedMinecraftServerResponse.class);

            if (response != null && response.getId() != null) {
                GameProfile result = new GameProfile(response.getId(), user.getName());

                if (response.getProperties() != null) {
                    result.getProperties().putAll(response.getProperties());
                }

                return result;
            } else {
                return null;
            }
        } catch (AuthenticationUnavailableException e) {
            throw e;
        } catch (AuthenticationException e) {
            return null;
        }
    }

    @Override
    public Map<MinecraftProfileTexture.Type, MinecraftProfileTexture> getTextures(GameProfile profile, boolean requireSecure) {
        Property textureProperty = Iterables.getFirst(profile.getProperties().get("textures"), null);
        if (textureProperty == null) return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();

        if (!textureProperty.hasSignature()) {
            LOGGER.error("Signature is missing from textures payload");
            return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
        }

        if (!textureProperty.isSignatureValid(publicKey)) {
            LOGGER.error("Textures payload has been tampered with (signature invalid)");
            return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
        }

        MinecraftTexturesPayload result;
        try {
            String json = new String(Base64.decodeBase64(textureProperty.getValue()), Charsets.UTF_8);
            result = gson.fromJson(json, MinecraftTexturesPayload.class);
        } catch (JsonParseException e) {
            LOGGER.error("Could not decode textures payload", e);
            return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
        }

        if (result.getProfileId() == null || !result.getProfileId().equals(profile.getId())) {
            LOGGER.error("Decrypted textures payload was for another user (expected id {} but was for {})", profile.getId(), result.getProfileId());
            return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
        }

        if (result.getProfileName() == null || !result.getProfileName().equals(profile.getName())) {
            LOGGER.error("Decrypted textures payload was for another user (expected name {} but was for {})", profile.getName(), result.getProfileName());
            return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
        }

        if (requireSecure) {
            if (result.isPublic()) {
                LOGGER.error("Decrypted textures payload was public but we require secure data");
                return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
            }

            Calendar limit = Calendar.getInstance();
            limit.add(Calendar.DATE, -1);
            Date validFrom = new Date(result.getTimestamp());

            if (validFrom.before(limit.getTime())) {
                LOGGER.error("Decrypted textures payload is too old ({0}, but we need it to be at least {1})", validFrom, limit);
                return new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>();
            }
        }

        return result.getTextures() == null ? new HashMap<MinecraftProfileTexture.Type, MinecraftProfileTexture>() : result.getTextures();
    }

    @Override
    public GameProfile fillProfileProperties(GameProfile profile) {
        if (profile.getId() == null) {
            return profile;
        }

        try {
            URL url = HttpAuthenticationService.constantURL(BASE_URL + "profile/" + UUIDTypeAdapter.fromUUID(profile.getId()));
            MinecraftProfilePropertiesResponse response = getAuthenticationService().makeRequest(url, null, MinecraftProfilePropertiesResponse.class);

            if (response == null) {
                LOGGER.debug("Couldn't fetch profile properties for " + profile + " as the profile does not exist");
                return profile;
            } else {
                LOGGER.debug("Successfully fetched profile properties for " + profile);
                GameProfile result = new GameProfile(response.getId(), response.getName());
                result.getProperties().putAll(response.getProperties());
                profile.getProperties().putAll(response.getProperties());
                return result;
            }
        } catch (AuthenticationException e) {
            LOGGER.warn("Couldn't look up profile properties for " + profile, e);
            return profile;
        }
    }

    @Override
    public YggdrasilAuthenticationService getAuthenticationService() {
        return (YggdrasilAuthenticationService) super.getAuthenticationService();
    }
}
