package org.spigotmc.authlib;

import org.spigotmc.authlib.minecraft.MinecraftSessionService;

public interface AuthenticationService {
    /**
     * Creates a relevant {@link org.spigotmc.authlib.UserAuthentication} designed for this authentication service.
     * <p />
     * Certain Authentication Services may have restrictions as to which {@link Agent}s are supported.
     * Please consult their javadoc for more information.
     *
     * @param agent Game agent to authenticate for
     * @throws java.lang.IllegalArgumentException Agent is null or not allowed for this AuthenticationService
     * @return New user authenticator
     */
    public UserAuthentication createUserAuthentication(Agent agent);

    /**
     * Creates a relevant {@link org.spigotmc.authlib.minecraft.MinecraftSessionService} designed for this authentication service.
     * </p>
     * This is a Minecraft specific service and is not relevant to any other game agent.
     *
     * @return New minecraft session service
     */
    public MinecraftSessionService createMinecraftSessionService();

    /**
     * Creates a relevant {@link org.spigotmc.authlib.GameProfileRepository} designed for this authentication service.
     *
     * @return New profile repository
     */
    public GameProfileRepository createProfileRepository();
}
