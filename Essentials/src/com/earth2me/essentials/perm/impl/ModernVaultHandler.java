package com.earth2me.essentials.perm.impl;

import java.util.Arrays;
import java.util.List;

public class ModernVaultHandler extends AbstractVaultHandler {
    private final List<String> supportedPlugins = Arrays.asList("PermissionsEx", "LuckPerms");

    @Override
    protected boolean emulateWildcards() {
        return false;
    }

    @Override
    public boolean tryProvider() {
        return super.canLoad() && supportedPlugins.contains(getEnabledPermsPlugin());
    }
}
