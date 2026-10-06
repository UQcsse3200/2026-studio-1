package com.csse3200.game.components.npc;

import com.csse3200.game.components.Component;

/** Stores the name assigned to an NPC. */
public class NameComponent extends Component {
  private String name;

  /**
   * Creates a name component.
   *
   * @param name name assigned to the NPC
   */
  public NameComponent(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Name must not be null or blank.");
    }
    this.name = name;
  }

  /**
   * Gets the NPC's name.
   *
   * @return NPC name
   */
  public String getName() {
    return name;
  }

  /**
   * Updates the NPC's name.
   *
   * @param name new NPC name
   */
  public void setName(String name) {
    if (name == null || name.isBlank()) {
      throw new IllegalArgumentException("Name must not be null or blank.");
    }
    this.name = name;
  }
}
