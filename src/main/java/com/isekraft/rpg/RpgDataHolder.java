package com.isekraft.rpg;
import net.minecraft.nbt.NbtCompound;

/**
 * @deprecated Artık kullanılmıyor. PlayerRpgManager HashMap tabanlı storage
 * ile çalışıyor. Bu interface yalnızca derleme uyumluluğu için tutuluyor.
 * Bir sonraki major versiyonda kaldırılacak.
 */
@Deprecated(forRemoval = true)
public interface RpgDataHolder {
    default NbtCompound isekraft_getRpgData() { return new NbtCompound(); }
    default void isekraft_setRpgData(NbtCompound data) {}
}
