package com.isekraft;

import com.isekraft.block.ModBlocks;
import com.isekraft.block.entity.ModBlockEntities;
import com.isekraft.entity.ModEntities;
import com.isekraft.item.ModItems;
import com.isekraft.armor.ModArmors;
import com.isekraft.network.ModPackets;
import com.isekraft.command.IsekaiCommand;
import com.isekraft.world.DemonLordEvent;
import com.isekraft.world.KillEventHandler;
import com.isekraft.rarity.RarityDropHandler;
import com.isekraft.equipment.EquipmentDropHandler;
import com.isekraft.world.FirstJoinHandler;
import com.isekraft.world.ModWorldGen;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class IseKraftMod implements ModInitializer {
    public static final String MOD_ID = "isekraft";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("[IseKraft] Initializing...");
        ModItems.register();
        ModArmors.register();
        ModBlocks.register();
        ModBlockEntities.register();
        ModEntities.register();
        ModPackets.register();
        ModWorldGen.registerCastleSpawn();
        FirstJoinHandler.register();
        IsekaiCommand.register();
        DemonLordEvent.register();
        KillEventHandler.register();
        RarityDropHandler.register();
        EquipmentDropHandler.register();
        LOGGER.info("[IseKraft] Ready! ✦");
    }
}
