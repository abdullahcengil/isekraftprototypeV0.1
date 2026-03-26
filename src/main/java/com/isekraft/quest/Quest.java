package com.isekraft.quest;

import net.minecraft.item.Item;

/**
 * Immutable quest definition. Instances live in QuestRegistry — never modified.
 *
 * Types:
 *   KILL   — kill [goal] of [targetId] entity type
 *   FETCH  — bring [goal] of [targetId] item to an Isekai NPC
 *   EXPLORE— reach a certain level milestone (progress = current level)
 *
 * Reward: flat XP + optional item drop (null = no item reward).
 */
public class Quest {

    public enum Type { KILL, FETCH, EXPLORE }

    public final String id;
    public final String title;
    public final String description;
    public final Type   type;
    public final String targetId;   // entity type path OR item registry path
    public final int    goal;       // required count
    public final int    xpReward;
    public final Item   itemReward; // nullable
    public final int    itemCount;
    public final int    levelRequired;

    public Quest(String id, String title, String description,
                 Type type, String targetId, int goal,
                 int xpReward, Item itemReward, int itemCount,
                 int levelRequired) {
        this.id            = id;
        this.title         = title;
        this.description   = description;
        this.type          = type;
        this.targetId      = targetId;
        this.goal          = goal;
        this.xpReward      = xpReward;
        this.itemReward    = itemReward;
        this.itemCount     = itemCount;
        this.levelRequired = levelRequired;
    }
}
