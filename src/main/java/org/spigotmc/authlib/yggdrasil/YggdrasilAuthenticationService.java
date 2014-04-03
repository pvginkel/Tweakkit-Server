package org.spigotmc.authlib.yggdrasil;

import com.google.gson.*;
import org.spigotmc.authlib.*;
import org.spigotmc.authlib.exceptions.AuthenticationException;
import org.spigotmc.authlib.exceptions.AuthenticationUnavailableException;
import org.spigotmc.authlib.exceptions.InvalidCredentialsException;
import org.spigotmc.authlib.exceptions.UserMigratedException;
import org.spigotmc.authlib.minecraft.MinecraftSessionService;
import org.spigotmc.authlib.properties.PropertyMap;
import org.spigotmc.authlib.yggdrasil.YggdrasilUserAuthentication;
import org.spigotmc.authlib.yggdrasil.response.Response;
import org.spigotmc.authlib.util.UUIDTypeAdapter;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.lang.reflect.Type;
import java.net.Proxy;
import java.net.URL;
import java.util.UUID;

public class YggdrasilAuthenticationService extends HttpAuthenticationService {
    private final String clientToken;
    private final Gson gson;

    public YggdrasilAuthenticationService(Proxy proxy, String clientToken) {
        super(proxy);
        this.clientToken = clientToken;
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(GameProfile.class, new GameProfileSerializer());
        builder.registerTypeAdapter(PropertyMap.class, new PropertyMap.Serializer());
        builder.registerTypeAdapter(UUID.class, new UUIDTypeAdapter());
        gson = builder.create();
    }

    @Override
    public UserAuthentication createUserAuthentication(Agent agent) {
        return new YggdrasilUserAuthentication(this, agent);
    }

    @Override
    public MinecraftSessionService createMinecraftSessionService() {
        return new YggdrasilMinecraftSessionService(this);
    }

    @Override
    public GameProfileRepository createProfileRepository() {
        return new YggdrasilGameProfileRepository(this);
    }

    protected <T extends Response> T makeRequest(URL url, Object input, Class<T> classOfT) throws AuthenticationException {
        try {
            String jsonResult = input == null ? performGetRequest(url) : performPostRequest(url, gson.toJson(input), "application/json");
            T result = gson.fromJson(jsonResult, classOfT);

            if (result == null) return null;

            if (StringUtils.isNotBlank(result.getError())) {
                if ("UserMigratedException".equals(result.getCause())) {
                    throw new UserMigratedException(result.getErrorMessage());
                } else if (result.getError().equals("ForbiddenOperationException")) {
                    throw new InvalidCredentialsException(result.getErrorMessage());
                } else {
                    throw new AuthenticationException(result.getErrorMessage());
                }
            }

            return result;
        } catch (IOException e) {
            throw new AuthenticationUnavailableException("Cannot contact authentication server", e);
        } catch (IllegalStateException e) {
            throw new AuthenticationUnavailableException("Cannot contact authentication server", e);
        } catch (JsonParseException e) {
            throw new AuthenticationUnavailableException("Cannot contact authentication server", e);
        }
    }

    public String getClientToken() {
        return clientToken;
    }

    private static class GameProfileSerializer implements JsonSerializer<GameProfile>, JsonDeserializer<GameProfile> {
        @Override
        public GameProfile deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
            JsonObject object = (JsonObject) json;
            UUID id = object.has("id") ? context.<UUID>deserialize(object.get("id"), UUID.class) : null;
            String name = object.has("name") ? object.getAsJsonPrimitive("name").getAsString() : null;
            return new GameProfile(id, name);
        }

        @Override
        public JsonElement serialize(GameProfile src, Type typeOfSrc, JsonSerializationContext context) {
            JsonObject result = new JsonObject();
            if (src.getId() != null) result.add("id", context.serialize(src.getId()));
            if (src.getName() != null) result.addProperty("name", src.getName());
            return result;
        }
    }
}
