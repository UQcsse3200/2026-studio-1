package com.csse3200.game.components.loot;

/**
 * Generates weapon items with properties based on weapon type and loot tier.
 */
public class WeaponGenerator {

    public WeaponItem generateWeapon(WeaponType weaponType, int tier) {
        if (weaponType == null) {
            throw new IllegalArgumentException("WeaponType must not be null.");
        }

        if (tier <= 0) {
            throw new IllegalArgumentException("Tier must be greater than 0.");
        }

        switch (weaponType) {
            case SWORD:
                return new WeaponItem(
                        "Basic Sword",
                        WeaponType.SWORD,
                        10 * tier,
                        1,
                        1);

            case BOW:
                return new WeaponItem(
                        "Basic Bow",
                        WeaponType.BOW,
                        7 * tier,
                        1,
                        1);

            default:
                throw new IllegalArgumentException("Unsupported weapon type.");
        }
    }
}
