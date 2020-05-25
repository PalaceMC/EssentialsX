package com.earth2me.essentials;

import com.earth2me.essentials.commands.IEssentialsCommand;
//import com.earth2me.essentials.signs.EssentialsSign;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.event.EventPriority;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Pattern;


public interface ISettings extends IConf {
    //boolean areSignsDisabled();

    String getNewPlayerKit();

    String getBackupCommand();

    long getBackupInterval();

    boolean isAlwaysRunBackup();

    BigDecimal getCommandCost(IEssentialsCommand cmd);

    BigDecimal getCommandCost(String label);

    String getCurrencySymbol();

    boolean isCurrencySymbolSuffixed();

    int getOversizedStackSize();

    int getDefaultStackSize();

    double getHealCooldown();

    Set<String> getMuteCommands();

    /**
     * @deprecated in favor of {@link Kits#getKits()}
     */
    @Deprecated
    ConfigurationSection getKits();

    /**
     * @deprecated in favor of {@link Kits#getKit(String)}
     */
    @Deprecated
    Map<String, Object> getKit(String kit);

    /**
     * @deprecated in favor of {@link Kits#addKit(String, List, long)}}
     */
    @Deprecated
    void addKit(String name, List<String> lines, long delay);

    boolean isSkippingUsedOneTimeKitsFromKitList();

    String getLocale();

    String getNewbieSpawn();

    boolean getPerWarpPermission();

    boolean getRespawnAtHome();

    Set getMultipleHomes();

    int getHomeLimit(String set);

    int getHomeLimit(User user);

    int getSpawnMobLimit();

    BigDecimal getStartingBalance();

    boolean isTeleportSafetyEnabled();

    boolean isForceDisableTeleportSafety();

    boolean isTeleportPassengerDismount();

    double getTeleportCooldown();

    double getTeleportDelay();

    boolean hidePermissionlessHelp();

    boolean isCommandDisabled(String label);

    boolean isCommandOverridden(String name);

    boolean isDebug();

    boolean isEcoDisabled();

    @Deprecated
    boolean isTradeInStacks(int id);

    boolean isTradeInStacks(Material type);

    List<Material> itemSpawnBlacklist();

    //List<EssentialsSign> enabledSigns();

    boolean permissionBasedItemSpawn();

    boolean showNonEssCommandsInHelp();

    boolean warnOnSmite();

    BigDecimal getMaxMoney();

    BigDecimal getMinMoney();

    boolean isEcoLogEnabled();

    boolean isEcoLogUpdateEnabled();

    boolean removeGodOnDisconnect();

    long getAutoAfk();

    long getAutoAfkKick();

    boolean getFreezeAfkPlayers();

    boolean cancelAfkOnMove();

    boolean cancelAfkOnInteract();

    boolean sleepIgnoresAfkPlayers();

    boolean isAfkListName();

    String getAfkListName();

    boolean broadcastAfkMessage();

    boolean areDeathMessagesEnabled();

    void setDebug(boolean debug);

    Set<String> getNoGodWorlds();

    boolean getUpdateBedAtDaytime();

    boolean allowUnsafeEnchantments();

    boolean getRepairEnchanted();

    boolean isWorldTeleportPermissions();

    boolean isWorldHomePermissions();

    boolean registerBackInListener();

    boolean getDisableItemPickupWhileAfk();

    EventPriority getRespawnPriority();

    EventPriority getSpawnJoinPriority();

    long getTpaAcceptCancellation();

    long getTeleportInvulnerability();

    boolean isTeleportInvulnerability();

    long getLoginAttackDelay();

    //int getSignUsePerSecond();

    double getMaxFlySpeed();

    double getMaxWalkSpeed();

    long getEconomyLagWarning();

    long getPermissionsLagWarning();

    long getMaxTempban();

    Map<String, Object> getListGroupConfig();

    int getMaxUserCacheCount();

    boolean isDropItemsIfFull();

    BigDecimal getMinimumPayAmount();

    boolean isMilkBucketEasterEggEnabled();

    boolean isSendFlyEnableOnJoin();

    boolean isWorldTimePermissions();

    boolean isSpawnOnJoin();

    List<String> getSpawnOnJoinGroups();

    boolean isUserInSpawnOnJoinGroup(IUser user);

    boolean isTeleportToCenterLocation();

    boolean isCommandCooldownsEnabled();

    boolean isWorldChangeFlyResetEnabled();

    boolean isWorldChangeSpeedResetEnabled();

    long getCommandCooldownMs(String label);

    Entry<Pattern, Long> getCommandCooldownEntry(String label);

    boolean isCommandCooldownPersistent(String label);

    boolean isNpcsInBalanceRanking();

    NumberFormat getCurrencyFormat();

    //List<EssentialsSign> getUnprotectedSignNames();

    boolean isPastebinCreateKit();

    boolean isAllowBulkBuySell();

    int getMotdDelay();

    boolean isDirectHatAllowed();

    List<String> getDefaultEnabledConfirmCommands();

    boolean isConfirmCommandEnabledByDefault(String commandName);

    boolean isTeleportBackWhenFreedFromJail();

    boolean isCompassTowardsHomePerm();

    boolean isAllowWorldInBroadcastworld();

    String getItemDbType();

    boolean isForceEnableRecipe();

    //boolean allowOldIdSigns();

    boolean isWaterSafe();

    boolean isSafeUsermap();

    boolean logCommandBlockCommands();

    double getMaxProjectileSpeed();

    boolean isRemovingEffectsOnHeal();

    boolean isSpawnIfNoHome();

}
