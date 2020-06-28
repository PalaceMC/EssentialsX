package com.earth2me.essentials;

import com.earth2me.essentials.commands.IEssentialsCommand;
import com.earth2me.essentials.messaging.IMessageRecipient;
import com.earth2me.essentials.register.payment.Method;
import com.earth2me.essentials.register.payment.Methods;
import com.earth2me.essentials.utils.DateUtil;
import com.earth2me.essentials.utils.FormatUtil;
import com.earth2me.essentials.utils.NumberUtil;
import net.ess3.api.IEssentials;
import net.ess3.api.MaxMoneyException;
import net.ess3.api.events.AfkStatusChangeEvent;
import net.ess3.api.events.JailStatusChangeEvent;
import net.ess3.api.events.MuteStatusChangeEvent;
import net.ess3.api.events.UserBalanceUpdateEvent;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.annotation.Nonnull;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

import static com.earth2me.essentials.I18n.tl;


public class User extends UserData implements Comparable<User>, IMessageRecipient, net.ess3.api.IUser {
    private transient UUID teleportRequester;
    private transient boolean teleportRequestHere;
    private transient Location teleportLocation;
    private transient boolean vanished;
    private transient final AsyncTeleport teleport;
    private transient final Teleport legacyTeleport;
    private transient long teleportRequestTime;
    private transient long lastOnlineActivity;
    private transient long lastActivity = System.currentTimeMillis();
    private boolean hidden = false;
    private boolean rightClickJump = false;
    private transient Location afkPosition = null;
    private boolean invSee = false;
    private boolean recipeSee = false;
    private boolean enderSee = false;
    private transient long teleportInvulnerabilityTimestamp = 0;
    private String afkMessage;
    private long afkSince;
    private final Map<User, BigDecimal> confirmingPayments = new WeakHashMap<>();
    private String confirmingClearCommand;

    public User(final Player base, final IEssentials ess) {
        super(base, ess);
        teleport = new AsyncTeleport(this, ess);
        legacyTeleport = new Teleport(this, ess);
        if (isAfk()) {
            afkPosition = this.getLocation();
        }
        if (this.getBase().isOnline()) {
            lastOnlineActivity = System.currentTimeMillis();
        }
    }

    @Override
    public boolean isAuthorized(final IEssentialsCommand cmd) {
        return isAuthorized(cmd, "essentials.");
    }

    @Override
    public boolean isAuthorized(final IEssentialsCommand cmd, final String permissionPrefix) {
        return isAuthorized(permissionPrefix + (cmd.getName().equals("r") ? "msg" : cmd.getName()));
    }

    @Override
    public boolean isAuthorized(final String node) {
        final boolean result = isAuthorizedCheck(node);
        if (ess.getSettings().isDebug()) {
            ess.getLogger().log(Level.INFO, "checking if " + base.getName() + " has " + node + " - " + result);
        }
        return result;
    }

    @Override
    public boolean isPermissionSet(final String node) {
        return isPermSetCheck(node);
    }

    private boolean isAuthorizedCheck(final String node) {

        /*if (base instanceof OfflinePlayer) {
            return false;
        }*/

        try {
            return ess.getPermissionsHandler().hasPermission(base, node);
        } catch (Exception ex) {
            if (ess.getSettings().isDebug()) {
                ess.getLogger().log(Level.SEVERE, "Permission System Error: " + ess.getPermissionsHandler().getName() + " returned: " + ex.getMessage(), ex);
            } else {
                ess.getLogger().log(Level.SEVERE, "Permission System Error: " + ess.getPermissionsHandler().getName() + " returned: " + ex.getMessage());
            }

            return false;
        }
    }

    private boolean isPermSetCheck(final String node) {
        if (!base.isOnline()) {
            return false;
        }

        try {
            return ess.getPermissionsHandler().isPermissionSet(base, node);
        } catch (Exception ex) {
            if (ess.getSettings().isDebug()) {
                ess.getLogger().log(Level.SEVERE, "Permission System Error: " + ess.getPermissionsHandler().getName() + " returned: " + ex.getMessage(), ex);
            } else {
                ess.getLogger().log(Level.SEVERE, "Permission System Error: " + ess.getPermissionsHandler().getName() + " returned: " + ex.getMessage());
            }

            return false;
        }
    }

    @Override
    public void healCooldown() throws Exception {
        final Calendar now = new GregorianCalendar();
        if (getLastHealTimestamp() > 0) {
            final double cooldown = ess.getSettings().getHealCooldown();
            final Calendar cooldownTime = new GregorianCalendar();
            cooldownTime.setTimeInMillis(getLastHealTimestamp());
            cooldownTime.add(Calendar.SECOND, (int) cooldown);
            cooldownTime.add(Calendar.MILLISECOND, (int) ((cooldown * 1000.0) % 1000.0));
            if (cooldownTime.after(now) && !isAuthorized("essentials.heal.cooldown.bypass")) {
                throw new Exception(tl("timeBeforeHeal", DateUtil.formatDateDiff(cooldownTime.getTimeInMillis())));
            }
        }
        setLastHealTimestamp(now.getTimeInMillis());
    }

    @Override
    public void giveMoney(final BigDecimal value) throws MaxMoneyException {
        giveMoney(value, null);
    }

    @Override
    public void giveMoney(final BigDecimal value, final CommandSource initiator) throws MaxMoneyException {
        giveMoney(value, initiator, UserBalanceUpdateEvent.Cause.UNKNOWN);
    }

    public void giveMoney(final BigDecimal value, final CommandSource initiator, UserBalanceUpdateEvent.Cause cause) throws MaxMoneyException {
        if (value.signum() == 0) {
            return;
        }
        setMoney(getMoney().add(value), cause);
        sendMessage(tl("addedToAccount", NumberUtil.displayCurrency(value, ess)));
        if (initiator != null) {
            initiator.sendMessage(tl("addedToOthersAccount", NumberUtil.displayCurrency(value, ess), getDisplayName(), NumberUtil.displayCurrency(getMoney(), ess)));
        }
    }

    @Override
    public void payUser(final User receiver, final BigDecimal value) throws Exception {
        payUser(receiver, value, UserBalanceUpdateEvent.Cause.UNKNOWN);
    }

    public void payUser(final User receiver, final BigDecimal value, UserBalanceUpdateEvent.Cause cause) throws Exception {
        if (value.compareTo(BigDecimal.ZERO) < 1) {
            throw new Exception(tl("payMustBePositive"));
        }

        if (canAfford(value)) {
            setMoney(getMoney().subtract(value), cause);
            receiver.setMoney(receiver.getMoney().add(value), cause);
            sendMessage(tl("moneySentTo", NumberUtil.displayCurrency(value, ess), receiver.getDisplayName()));
            receiver.sendMessage(tl("moneyReceivedFrom", NumberUtil.displayCurrency(value, ess), getDisplayName()));
        } else {
            throw new ChargeException(tl("notEnoughMoney", NumberUtil.displayCurrency(value, ess)));
        }
    }

    @Override
    public void takeMoney(final BigDecimal value) {
        takeMoney(value, null);
    }

    @Override
    public void takeMoney(final BigDecimal value, final CommandSource initiator) {
        takeMoney(value, initiator, UserBalanceUpdateEvent.Cause.UNKNOWN);
    }

    public void takeMoney(final BigDecimal value, final CommandSource initiator, UserBalanceUpdateEvent.Cause cause) {
        if (value.signum() == 0) {
            return;
        }
        try {
            setMoney(getMoney().subtract(value), cause);
        } catch (MaxMoneyException ex) {
            ess.getLogger().log(Level.WARNING, "Invalid call to takeMoney, total balance can't be more than the max-money limit.", ex);
        }
        sendMessage(tl("takenFromAccount", NumberUtil.displayCurrency(value, ess)));
        if (initiator != null) {
            initiator.sendMessage(tl("takenFromOthersAccount", NumberUtil.displayCurrency(value, ess), getDisplayName(), NumberUtil.displayCurrency(getMoney(), ess)));
        }
    }

    @Override
    public boolean canAfford(final BigDecimal cost) {
        return canAfford(cost, true);
    }

    public boolean canAfford(final BigDecimal cost, final boolean permCheck) {
        if (cost.signum() <= 0) {
            return true;
        }
        final BigDecimal remainingBalance = getMoney().subtract(cost);
        if (!permCheck || isAuthorized("essentials.eco.loan")) {
            return (remainingBalance.compareTo(ess.getSettings().getMinMoney()) >= 0);
        }
        return (remainingBalance.signum() >= 0);
    }

    public void dispose() {
        ess.runTaskAsynchronously(this::_dispose);
    }

    private void _dispose() {
        cleanup();
    }

    @Override
    public Boolean canSpawnItem(final Material material) {
        if (ess.getSettings().permissionBasedItemSpawn()) {
            final String name = material.toString().toLowerCase(Locale.ENGLISH).replace("_", "");

            if (isAuthorized("essentials.itemspawn.item-all") || isAuthorized("essentials.itemspawn.item-" + name)) return true;
        }

        return isAuthorized("essentials.itemspawn.exempt") || !ess.getSettings().itemSpawnBlacklist().contains(material);
    }

    @Override
    public void setLastLocation() {
        setLastLocation(this.getLocation());
    }

    @Override
    public void setLogoutLocation() {
        setLogoutLocation(this.getLocation());
    }

    @Override
    public void requestTeleport(final User player, final boolean here) {
        teleportRequestTime = System.currentTimeMillis();
        teleportRequester = player == null ? null : player.getBase().getUniqueId();
        teleportRequestHere = here;
        if (player == null) {
            teleportLocation = null;
        } else {
            teleportLocation = here ? player.getLocation() : this.getLocation();
        }
    }

    @Override
    public boolean hasOutstandingTeleportRequest() {
        if (getTeleportRequest() != null) { // Player has outstanding teleport request.
            long timeout = ess.getSettings().getTpaAcceptCancellation();
            if (timeout != 0) {
                if ((System.currentTimeMillis() - getTeleportRequestTime()) / 1000 <= timeout) { // Player has outstanding request
                    return true;
                } else { // outstanding request expired.
                    requestTeleport(null, false);
                    return false;
                }
            } else { // outstanding request does not expire
                return true;
            }
        }
        return false;
    }

    public UUID getTeleportRequest() {
        return teleportRequester;
    }

    public boolean isTpRequestHere() {
        return teleportRequestHere;
    }

    public Location getTpRequestLocation() {
        return teleportLocation;
    }

    public String getDisplayName() {
        return getBase().getDisplayName();
    }

    @Override
    public AsyncTeleport getAsyncTeleport() {
        return teleport;
    }

    /**
     * @deprecated This API is not asynchronous. Use {@link User#getAsyncTeleport()}
     */
    @Override
    @Deprecated
    public Teleport getTeleport() {
        return legacyTeleport;
    }

    public long getLastOnlineActivity() {
        return lastOnlineActivity;
    }

    public void setLastOnlineActivity(final long timestamp) {
        lastOnlineActivity = timestamp;
    }

    @Override
    public BigDecimal getMoney() {
        final long start = System.nanoTime();
        final BigDecimal value = _getMoney();
        final long elapsed = System.nanoTime() - start;
        if (elapsed > ess.getSettings().getEconomyLagWarning()) {
            ess.getLogger().log(Level.INFO, "Lag Notice - Slow Economy Response - Request took over {0}ms!", elapsed / 1000000.0);
        }
        return value;
    }

    private BigDecimal _getMoney() {
        if (ess.getSettings().isEcoDisabled()) {
            if (ess.getSettings().isDebug()) {
                ess.getLogger().info("Internal economy functions disabled, aborting balance check.");
            }
            return BigDecimal.ZERO;
        }
        if (Methods.hasMethod()) {
            try {
                final Method method = Methods.getMethod();
                if (!method.hasAccount(this.getName())) {
                    throw new Exception();
                }
                final Method.MethodAccount account = Methods.getMethod().getAccount(this.getName());
                return BigDecimal.valueOf(account.balance());
            } catch (Exception ignored) {
            }
        }
        return super.getMoney();
    }

    @Override
    public void setMoney(final BigDecimal value) throws MaxMoneyException {
        setMoney(value, UserBalanceUpdateEvent.Cause.UNKNOWN);
    }

    public void setMoney(final BigDecimal value, UserBalanceUpdateEvent.Cause cause) throws MaxMoneyException {
        if (ess.getSettings().isEcoDisabled()) {
            if (ess.getSettings().isDebug()) {
                ess.getLogger().info("Internal economy functions disabled, aborting balance change.");
            }
            return;
        }
        final BigDecimal oldBalance = _getMoney();

        UserBalanceUpdateEvent updateEvent = new UserBalanceUpdateEvent(this.getBase(), oldBalance, value, cause);
        ess.getServer().getPluginManager().callEvent(updateEvent);
        BigDecimal newBalance = updateEvent.getNewBalance();

        if (Methods.hasMethod()) {
            try {
                final Method method = Methods.getMethod();
                if (!method.hasAccount(this.getName())) {
                    throw new Exception();
                }
                final Method.MethodAccount account = Methods.getMethod().getAccount(this.getName());
                account.set(newBalance.doubleValue());
            } catch (Exception ignored) {
            }
        }
        super.setMoney(newBalance, true);
        Trade.log("Update", "Set", "API", getName(), new Trade(newBalance, ess), null, null, null, ess);
    }

    public void updateMoneyCache(final BigDecimal value) {
        if (ess.getSettings().isEcoDisabled()) {
            return;
        }
        if (Methods.hasMethod() && !super.getMoney().equals(value)) {
            try {
                super.setMoney(value, false);
            } catch (MaxMoneyException ex) {
                // We don't want to throw any errors here, just updating a cache
            }
        }
    }

    @Override
    public void setAfk(final boolean set) {
        setAfk(set, AfkStatusChangeEvent.Cause.UNKNOWN);
    }

    @Override
    public void setAfk(boolean set, AfkStatusChangeEvent.Cause cause) {
        final AfkStatusChangeEvent afkEvent = new AfkStatusChangeEvent(this, set, cause);
        ess.getServer().getPluginManager().callEvent(afkEvent);
        if (afkEvent.isCancelled()) {
            return;
        }

        this.getBase().setSleepingIgnored(this.isAuthorized("essentials.sleepingignored") || set && ess.getSettings().sleepIgnoresAfkPlayers());
        if (set && !isAfk()) {
            afkPosition = this.getLocation();
            this.afkSince = System.currentTimeMillis();
        } else if (!set && isAfk()) {
            afkPosition = null;
            this.afkMessage = null;
            this.afkSince = 0;
        }
        _setAfk(set);
        updateAfkListName();
    }

    private void updateAfkListName() {
        if (ess.getSettings().isAfkListName()) {
            if(isAfk()) {
                String afkName = ess.getSettings().getAfkListName().replace("{PLAYER}", getDisplayName()).replace("{USERNAME}", getName());
                getBase().setPlayerListName(afkName);
            } else {
                getBase().setPlayerListName(null);
            }
        }
    }

    @Deprecated
    public boolean toggleAfk() {
        return toggleAfk(AfkStatusChangeEvent.Cause.UNKNOWN);
    }

    public boolean toggleAfk(AfkStatusChangeEvent.Cause cause) {
        setAfk(!isAfk(), cause);
        return isAfk();
    }

    @Override
    public boolean isHidden() {
        return hidden;
    }

    public boolean isHidden(final Player player) {
        return hidden || !player.canSee(getBase());
    }

    @Override
    public void setHidden(final boolean hidden) {
        this.hidden = hidden;
        if (hidden) {
            setLastLogout(getLastOnlineActivity());
        }
    }

    public void checkJailTimeout(final long currentTime) {
        if (getJailTimeout() > 0 && getJailTimeout() < currentTime && isJailed()) {
            final JailStatusChangeEvent event = new JailStatusChangeEvent(this, null, false);
            ess.getServer().getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                setJailTimeout(0);
                setJailed(false);
                sendMessage(tl("haveBeenReleased"));
                setJail(null);
                if (ess.getSettings().isTeleportBackWhenFreedFromJail()) {
                    CompletableFuture<Boolean> future = new CompletableFuture<>();
                    getAsyncTeleport().back(future);
                    future.exceptionally(e -> {
                        getAsyncTeleport().respawn(null, TeleportCause.PLUGIN, new CompletableFuture<>());
                        return false;
                    });
                }
            }
        }
    }

    public void checkMuteTimeout(final long currentTime) {
        if (getMuteTimeout() > 0 && getMuteTimeout() < currentTime && isMuted()) {
            final MuteStatusChangeEvent event = new MuteStatusChangeEvent(this, null, false, getMuteTimeout(), getMuteReason());
            ess.getServer().getPluginManager().callEvent(event);

            if (!event.isCancelled()) {
                setMuteTimeout(0);
                sendMessage(tl("canTalkAgain"));
                setMuted(false);
                setMuteReason(null);
            }
        }
    }

    @Deprecated
    public void updateActivity(final boolean broadcast) {
        updateActivity(broadcast, AfkStatusChangeEvent.Cause.UNKNOWN);
    }

    public void updateActivity(final boolean broadcast, AfkStatusChangeEvent.Cause cause) {
        if (isAfk()) {
            setAfk(false, cause);
            if (broadcast && !isHidden()) {
                final String msg = tl("userIsNotAway", getDisplayName());
                final String selfmsg = tl("userIsNotAwaySelf", getDisplayName());
                if (!msg.isEmpty() && ess.getSettings().broadcastAfkMessage()) {
                    // exclude user from receiving general AFK announcement in favor of personal message
                    ess.broadcastMessage(this, msg, u -> u == this);
                }
                if (!selfmsg.isEmpty()) {
                    this.sendMessage(selfmsg);
                }
            }
        }
        lastActivity = System.currentTimeMillis();
    }

    public void updateActivityOnMove(final boolean broadcast) {
        if(ess.getSettings().cancelAfkOnMove()) {
            updateActivity(broadcast, AfkStatusChangeEvent.Cause.MOVE);
        }
    }

    public void updateActivityOnInteract(final boolean broadcast) {
        if(ess.getSettings().cancelAfkOnInteract()) {
            updateActivity(broadcast, AfkStatusChangeEvent.Cause.INTERACT);
        }
    }

    public void checkActivity() {
        // Graceful time before the first afk check call.
        if (System.currentTimeMillis() - lastActivity <= 10000) {
            return;
        }

        final long autoafkkick = ess.getSettings().getAutoAfkKick();
        if (autoafkkick > 0
            && lastActivity > 0 && (lastActivity + (autoafkkick * 1000)) < System.currentTimeMillis()
            && !isAuthorized("essentials.kick.exempt")
            && !isAuthorized("essentials.afk.kickexempt")) {
            final String kickReason = tl("autoAfkKickReason", autoafkkick / 60.0);
            lastActivity = 0;
            this.getBase().kickPlayer(kickReason);


            for (User user : ess.getOnlineUsers()) {
                if (user.isAuthorized("essentials.kick.notify")) {
                    user.sendMessage(tl("playerKicked", "Console", getName(), kickReason));
                }
            }
        }
        final long autoafk = ess.getSettings().getAutoAfk();
        if (!isAfk() && autoafk > 0 && lastActivity + autoafk * 1000 < System.currentTimeMillis() && isAuthorized("essentials.afk.auto")) {
            setAfk(true, AfkStatusChangeEvent.Cause.ACTIVITY);
            if (!isHidden()) {
                final String msg = tl("userIsAway", getDisplayName());
                final String selfmsg = tl("userIsAwaySelf", getDisplayName());
                if (!msg.isEmpty() && ess.getSettings().broadcastAfkMessage()) {
                    // exclude user from receiving general AFK announcement in favor of personal message
                    ess.broadcastMessage(this, msg, u -> u == this);
                }
                if (!selfmsg.isEmpty()) {
                    this.sendMessage(selfmsg);
                }
            }
        }
    }

    public Location getAfkPosition() {
        return afkPosition;
    }

    @Override
    public boolean isGodModeEnabled() {
        if (super.isGodModeEnabled()) {
            // This enables the no-god-in-worlds functionality where the actual player god mode state is never modified in disabled worlds,
            // but this method gets called every time the player takes damage. In the case that the world has god-mode disabled then this method
            // will return false and the player will take damage, even though they are in god mode (isGodModeEnabledRaw()).
            World world = getLocation().getWorld();
            if (world == null) return true; // not sure how to default this, but better safe than sorry I guess
            return !ess.getSettings().getNoGodWorlds().contains(world.getName());
        }
        if (isAfk()) {
            // Protect AFK players by representing them in a god mode state to render them invulnerable to damage.
            return ess.getSettings().getFreezeAfkPlayers();
        }
        return false;
    }

    public boolean isGodModeEnabledRaw() {
        return super.isGodModeEnabled();
    }

    @Override
    public String getGroup() {
        final String result = ess.getPermissionsHandler().getGroup(base);
        if (ess.getSettings().isDebug()) {
            ess.getLogger().log(Level.INFO, "looking up group name of " + base.getName() + " - " + result);
        }
        return result;
    }

    @Override
    public boolean inGroup(final String group) {
        final boolean result = ess.getPermissionsHandler().inGroup(base, group);
        if (ess.getSettings().isDebug()) {
            ess.getLogger().log(Level.INFO, "checking if " + base.getName() + " is in group " + group + " - " + result);
        }
        return result;
    }

    public long getTeleportRequestTime() {
        return teleportRequestTime;
    }

    public boolean isInvSee() {
        return invSee;
    }

    public void setInvSee(final boolean set) {
        invSee = set;
    }

    public boolean isEnderSee() {
        return enderSee;
    }

    public void setEnderSee(final boolean set) {
        enderSee = set;
    }

    @Override
    public void enableInvulnerabilityAfterTeleport() {
        final long time = ess.getSettings().getTeleportInvulnerability();
        if (time > 0) {
            teleportInvulnerabilityTimestamp = System.currentTimeMillis() + time;
        }
    }

    @Override
    public void resetInvulnerabilityAfterTeleport() {
        if (teleportInvulnerabilityTimestamp != 0 && teleportInvulnerabilityTimestamp < System.currentTimeMillis()) {
            teleportInvulnerabilityTimestamp = 0;
        }
    }

    @Override
    public boolean hasInvulnerabilityAfterTeleport() {
        return teleportInvulnerabilityTimestamp != 0 && teleportInvulnerabilityTimestamp >= System.currentTimeMillis();
    }

    public boolean canInteractVanished() {
        return isAuthorized("essentials.vanish.interact");
    }

    @Override
    public boolean isVanished() {
        return vanished;
    }

    @Override
    public void setVanished(final boolean set) {
        vanished = set;
        if (set) {
            for (User user : ess.getOnlineUsers()) {
                if (!user.isAuthorized("essentials.vanish.see")) {
                    user.getBase().hidePlayer(ess, getBase());
                }
            }
            setHidden(true);
            ess.getVanishedPlayersNew().add(getName());
            if (isAuthorized("essentials.vanish.effect")) {
                this.getBase().addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 1, false));
            }
        } else {
            for (Player p : ess.getOnlinePlayers()) {
                p.showPlayer(ess, getBase());
            }
            setHidden(false);
            ess.getVanishedPlayersNew().remove(getName());
            if (isAuthorized("essentials.vanish.effect")) {
                this.getBase().removePotionEffect(PotionEffectType.INVISIBILITY);
            }
        }
    }

    /*public boolean checkSignThrottle() {
        if (isSignThrottled()) {
            return true;
        }
        updateThrottle();
        return false;
    }

    public boolean isSignThrottled() {
        final long minTime = lastThrottledAction + (1000 / ess.getSettings().getSignUsePerSecond());
        return (System.currentTimeMillis() < minTime);
    }

    public void updateThrottle() {
        lastThrottledAction = System.currentTimeMillis();
    }*/

    public boolean isFlyClickJump() {
        return rightClickJump;
    }

    public void setRightClickJump(boolean rightClickJump) {
        this.rightClickJump = rightClickJump;
    }

    public boolean isRecipeSee() {
        return recipeSee;
    }

    public void setRecipeSee(boolean recipeSee) {
        this.recipeSee = recipeSee;
    }

    @Override
    public void sendMessage(String message) {
        if (!message.isEmpty()) {
            base.sendMessage(message);
        }
    }

    @Override
    public int compareTo(final User other) {
        return FormatUtil.stripFormat(getDisplayName()).compareToIgnoreCase(FormatUtil.stripFormat(other.getDisplayName()));
    }

    @Override
    public boolean equals(final Object object) {
        if (!(object instanceof User)) {
            return false;
        }
        return this.getName().equalsIgnoreCase(((User) object).getName());

    }

    @Override
    public int hashCode() {
        return this.getName().hashCode();
    }

    @Override
    public CommandSource getSource() {
        return new CommandSource(getBase());
    }

    @Override
    public String getName() {
        return this.getBase().getName();
    }

    @Override public boolean isReachable() {
        return getBase().isOnline();
    }

    @Override
    public String getAfkMessage() {
        return this.afkMessage;
    }

    @Override
    public void setAfkMessage(String message) {
        if (isAfk()) {
            this.afkMessage = message;
        }
    }

    @Override
    public long getAfkSince() {
        return afkSince;
    }

    @Override
    public Map<User, BigDecimal> getConfirmingPayments() {
        return confirmingPayments;
    }

    public String getConfirmingClearCommand() {
        return confirmingClearCommand;
    }

    public void setConfirmingClearCommand(String command) {
        this.confirmingClearCommand = command;
    }

    /**
     * Returns the {@link ItemStack} in the main hand or off-hand. If the main hand is empty then the offhand item is returned.
     */
    @Nonnull
    public ItemStack getItemInHand() {
        PlayerInventory inventory = getBase().getInventory();
        // an empty hand is actually represented by a hand with a single block of air in it...
        return inventory.getItemInMainHand().getType().isAir() ? inventory.getItemInOffHand() : inventory.getItemInMainHand();
    }
}
