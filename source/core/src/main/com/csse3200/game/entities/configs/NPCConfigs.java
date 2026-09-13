package com.csse3200.game.entities.configs;

import com.csse3200.game.entities.configs.enemies.RangedSkeletonConfig;
import com.csse3200.game.entities.configs.enemies.SkeletonConfig;

/** Defines all NPC configs to be loaded by the NPC Factory. */
public class NPCConfigs {
  public BaseEntityConfig ghost = new BaseEntityConfig();
  public GhostKingConfig ghostKing = new GhostKingConfig();
  public SkeletonConfig skeleton = new SkeletonConfig();
  public RangedSkeletonConfig rangedSkeleton = new RangedSkeletonConfig();
}
