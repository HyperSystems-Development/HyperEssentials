package com.hyperessentials;

/**
 * Centralized permission node definitions for HyperEssentials.
 */
public final class Permissions {

    private Permissions() {}

    public static final String ROOT = "hyperessentials";

    // Homes
    public static final String HOME = ROOT + ".home";
    public static final String HOME_SET = HOME + ".set";
    public static final String HOME_DELETE = HOME + ".delete";
    public static final String HOME_LIST = HOME + ".list";
    public static final String HOME_GUI = HOME + ".gui";
    public static final String HOME_SHARE = HOME + ".share";
    public static final String HOME_UNLIMITED = HOME + ".unlimited";

    // Warps
    public static final String WARP = ROOT + ".warp";
    public static final String WARP_SET = WARP + ".set";
    public static final String WARP_DELETE = WARP + ".delete";
    public static final String WARP_LIST = WARP + ".list";
    public static final String WARP_INFO = WARP + ".info";

    // Spawns
    public static final String SPAWN = ROOT + ".spawn";
    public static final String SPAWN_SET = SPAWN + ".set";
    public static final String SPAWN_DELETE = SPAWN + ".delete";
    public static final String SPAWN_LIST = SPAWN + ".list";
    public static final String SPAWN_INFO = SPAWN + ".info";

    // Teleport
    public static final String TPA = ROOT + ".tpa";
    public static final String TPAHERE = ROOT + ".tpahere";
    public static final String TPACCEPT = ROOT + ".tpaccept";
    public static final String TPDENY = ROOT + ".tpdeny";
    public static final String TPCANCEL = ROOT + ".tpcancel";
    public static final String TPTOGGLE = ROOT + ".tptoggle";
    public static final String BACK = ROOT + ".back";

    // Bypass
    public static final String BYPASS = ROOT + ".bypass";
    public static final String BYPASS_WARMUP = BYPASS + ".warmup";
    public static final String BYPASS_COOLDOWN = BYPASS + ".cooldown";
    public static final String BYPASS_LIMIT = BYPASS + ".limit";

    // Admin
    public static final String ADMIN = ROOT + ".admin";
    public static final String ADMIN_RELOAD = ADMIN + ".reload";
    public static final String ADMIN_SETTINGS = ADMIN + ".settings";

    // Future module permissions
    public static final String KIT = ROOT + ".kit";
    public static final String VANISH = ROOT + ".vanish";
    public static final String FREEZE = ROOT + ".freeze";
    public static final String MUTE = ROOT + ".mute";
    public static final String RTP = ROOT + ".rtp";
}
