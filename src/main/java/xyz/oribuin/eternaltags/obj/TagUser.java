package xyz.oribuin.eternaltags.obj;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class TagUser {

    private final @NotNull UUID player;
    private @Nullable String activeTag;
    private boolean usingDefaultTag;
    private @NotNull Set<String> favourites;

    /**
     * Create a new TagUser object.
     *
     * @param player The player to create the object for.
     */
    public TagUser(@NotNull UUID player) {
        this.player = player;
        this.activeTag = null;
        this.usingDefaultTag = false;
        this.favourites = new HashSet<>();
    }

    /**
     * Create a new TagUser object.
     *
     * @param player The player to create the object for.
     */
    public TagUser(@NotNull Player player) {
        this.player = player.getUniqueId();
        this.activeTag = null;
        this.usingDefaultTag = false;
        this.favourites = new HashSet<>();
    }

    /**
     * @return The UUID of the player this object represents.
     */
    @NotNull
    public UUID getPlayer() {
        return this.player;
    }


    /**
     * @return The active tag of the player.
     */
    @Nullable
    public String getActiveTag() {
        return this.activeTag;
    }

    /**
     * @param activeTag Set the active tag of the player.
     */
    public void setActiveTag(@Nullable String activeTag) {
        this.activeTag = activeTag;
    }

    /**
     * @return Whether the player is using the default tag.
     */
    public boolean isUsingDefaultTag() {
        return this.usingDefaultTag;
    }

    /**
     * @param usingDefaultTag Set whether the player is using the default tag.
     */
    public void setUsingDefaultTag(boolean usingDefaultTag) {
        this.usingDefaultTag = usingDefaultTag;
    }

    /**
     * @return The player's favourite tags.
     */
    public @NotNull Set<String> getFavourites() {
        return this.favourites;
    }

    /**
     * @param favourites Set the player's favourite tags.
     */
    public void setFavourites(@NotNull Set<String> favourites) {
        this.favourites = favourites;
    }

}
