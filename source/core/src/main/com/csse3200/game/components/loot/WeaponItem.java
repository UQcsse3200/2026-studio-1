package com.csse3200.game.components.loot;

/**
 * Represents a weapon item that can be stored in the player's inventory.
 *
 * <p>A weapon has the common properties of an Item, as well as a weapon type
 * and damage value.
 */
public class WeaponItem extends Item {

    private final WeaponType weaponType;
    private final int damage;

    /**
     * Creates a weapon item.
     *
     * @param name the name of the weapon
     * @param weaponType the type of weapon
     * @param damage the damage dealt by the weapon
     * @param quantity the number of weapons in the stack
     * @param maxQuantity the maximum number of weapons that can be stacked
     */
    public WeaponItem(
            String name,
            WeaponType weaponType,
            int damage,
            int quantity,
            int maxQuantity) {

        super(name, ItemType.WEAPON, quantity, maxQuantity);

        if (weaponType == null) {
            throw new IllegalArgumentException("WeaponType must not be null.");
        }

        if (damage < 0) {
            throw new IllegalArgumentException("Damage must not be negative.");
        }

        this.weaponType = weaponType;
        this.damage = damage;
    }

    public WeaponType getWeaponType() {
        return weaponType;
    }

    public int getDamage() {
        return damage;
    }
}
