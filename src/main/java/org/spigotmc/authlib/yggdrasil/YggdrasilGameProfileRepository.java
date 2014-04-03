package org.spigotmc.authlib.yggdrasil;

import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import org.spigotmc.authlib.*;
import org.spigotmc.authlib.exceptions.AuthenticationException;
import org.spigotmc.authlib.yggdrasil.ProfileNotFoundException;
import org.spigotmc.authlib.yggdrasil.YggdrasilAuthenticationService;
import org.spigotmc.authlib.yggdrasil.response.ProfileSearchResultsResponse;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Set;

public class YggdrasilGameProfileRepository implements GameProfileRepository {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String BASE_URL = "https://api.mojang.com/";
    private static final String SEARCH_PAGE_URL = BASE_URL + "profiles/page/";
    private static final int MAX_FAIL_COUNT = 3;
    private static final int DELAY_BETWEEN_PAGES = 100;
    private static final int DELAY_BETWEEN_FAILURES = 750;

    private final YggdrasilAuthenticationService authenticationService;

    public YggdrasilGameProfileRepository(YggdrasilAuthenticationService authenticationService) {
        this.authenticationService = authenticationService;
    }

    @Override
    public void findProfilesByNames(String[] names, Agent agent, ProfileLookupCallback callback) {
        Set<ProfileCriteria> criteria = Sets.newHashSet();

        for (String name : names) {
            if (!Strings.isNullOrEmpty(name)) {
                criteria.add(new ProfileCriteria(name, agent));
            }
        }

        Exception exception = null;
        Set<ProfileCriteria> request = Sets.newHashSet(criteria);
        int page = 1;
        int failCount = 0;

        while (!criteria.isEmpty()) {
            try {
                ProfileSearchResultsResponse response = authenticationService.makeRequest(HttpAuthenticationService.constantURL(SEARCH_PAGE_URL + page), request, ProfileSearchResultsResponse.class);
                failCount = 0;
                exception = null;

                if (response.getSize() == 0 || response.getProfiles().length == 0) {
                    LOGGER.debug("Page {} returned empty, aborting search", page);
                    break;
                } else {
                    LOGGER.debug("Page {} returned {} results of {}, parsing", page, response.getProfiles().length, response.getSize());

                    for (GameProfile profile : response.getProfiles()) {
                        LOGGER.debug("Successfully looked up profile {}", profile);
                        criteria.remove(new ProfileCriteria(profile.getName(), agent));
                        callback.onProfileLookupSucceeded(profile);
                    }

                    LOGGER.debug("Page {} successfully parsed", page);
                    page++;

                    try {
                        Thread.sleep(DELAY_BETWEEN_PAGES);
                    } catch (InterruptedException ignored) {}
                }
            } catch (AuthenticationException e) {
                exception = e;
                failCount++;

                if (failCount == MAX_FAIL_COUNT) {
                    break;
                } else {
                    try {
                        Thread.sleep(DELAY_BETWEEN_FAILURES);
                    } catch (InterruptedException ignored) {}
                }
            }
        }

        if (criteria.isEmpty()) {
            LOGGER.debug("Successfully found every profile requested");
        } else {
            LOGGER.debug("{} profiles were missing from search results", criteria.size());
            if (exception == null) {
                exception = new ProfileNotFoundException("Server did not find the requested profile");
            }
            for (ProfileCriteria profileCriteria : criteria) {
                callback.onProfileLookupFailed(new GameProfile(null, profileCriteria.getName()), exception);
            }
        }
    }

    private class ProfileCriteria {
        private final String name;
        private final String agent;

        private ProfileCriteria(String name, Agent agent) {
            this.name = name;
            this.agent = agent.getName();
        }

        public String getName() {
            return name;
        }

        public String getAgent() {
            return agent;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ProfileCriteria that = (ProfileCriteria) o;
            return agent.equals(that.agent) && name.toLowerCase().equals(that.name.toLowerCase());
        }

        @Override
        public int hashCode() {
            return 31 * name.toLowerCase().hashCode() + agent.hashCode();
        }

        @Override
        public String toString() {
            return new ToStringBuilder(this)
                    .append("agent", agent)
                    .append("name", name)
                    .toString();
        }
    }

}
