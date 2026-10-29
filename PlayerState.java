package com.echocity.velmar;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;

/** Хранилище сюжетного состояния игрока (флаги, заряды эха, счётчик перерождений, точка города). */
public final class PlayerState {
    public static final String PERSISTED = "PlayerPersisted";
    private static final String ROOT = "echocity";

    private PlayerState() {}

    public static CompoundTag tag(Player p) {
        CompoundTag data = p.getPersistentData();
        if (!data.contains(PERSISTED, 10)) data.put(PERSISTED, new CompoundTag());
        CompoundTag persisted = data.getCompound(PERSISTED);
        if (!persisted.contains(ROOT, 10)) persisted.put(ROOT, new CompoundTag());
        return persisted.getCompound(ROOT);
    }

    public static boolean flag(Player p, String f) { return tag(p).getBoolean("f_" + f); }
    public static void set(Player p, String f) { tag(p).putBoolean("f_" + f, true); }
    public static void unset(Player p, String f) { tag(p).remove("f_" + f); }

    public static int getInt(Player p, String k) { return tag(p).getInt(k); }
    public static void putInt(Player p, String k, int v) { tag(p).putInt(k, v); }

    public static boolean hasCity(Player p) { return tag(p).getBoolean("has_city"); }

    public static void setOrigin(Player p, BlockPos o) {
        CompoundTag t = tag(p);
        t.putBoolean("has_city", true);
        t.putInt("ox", o.getX()); t.putInt("oy", o.getY()); t.putInt("oz", o.getZ());
    }

    public static BlockPos origin(Player p) {
        CompoundTag t = tag(p);
        return new BlockPos(t.getInt("ox"), t.getInt("oy"), t.getInt("oz"));
    }

    public static void reset(Player p) {
        CompoundTag t = tag(p);
        BlockPos o = hasCity(p) ? origin(p) : null;
        for (String k : new java.util.ArrayList<>(t.getAllKeys())) t.remove(k);
        if (o != null) setOrigin(p, o);
    }

    public static void copy(Player from, Player to) {
        CompoundTag src = from.getPersistentData();
        if (src.contains(PERSISTED, 10)) to.getPersistentData().put(PERSISTED, src.getCompound(PERSISTED).copy());
    }
}
