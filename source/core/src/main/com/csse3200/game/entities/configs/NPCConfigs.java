package com.csse3200.game.entities.configs;

import com.csse3200.game.entities.configs.enemies.*;

/** Defines all NPC configs to be loaded by the NPC Factory. */
public class NPCConfigs {
  public BaseEntityConfig ghost = new BaseEntityConfig();
  public GhostKingConfig ghostKing = new GhostKingConfig();
  public SkeletonConfig skeleton = new SkeletonConfig();
  public RangedSkeletonConfig rangedSkeleton = new RangedSkeletonConfig();
  public CentaurConfig centaur = new CentaurConfig();
  public MinotaurConfig minotaur = new MinotaurConfig();
  public CyclopsConfig cyclops = new CyclopsConfig();
}
