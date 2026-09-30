package com.csse3200.game.components.npc;

import com.csse3200.game.components.Component;
import com.csse3200.game.components.EnemyType;

import java.util.Locale;

public class EnemyTypeComponent extends Component {
    private String enemyLabel;

    public EnemyTypeComponent(EnemyType enemyType) {
        setEnemyLabel(enemyType);
    }

    private void setEnemyLabel(EnemyType enemyType) {
        if (enemyType == null) {
            throw new IllegalArgumentException("EnemyType cannot be null.");
        }
        String enemy = enemyType.name();
        if (enemy.isEmpty()) {
            throw new IllegalArgumentException("EnemyType cannot have an empty name string");
        }
        if (enemy.charAt(0) == '_') {
            throw new IllegalArgumentException("Enemy type string cannot have a leading underscore");
        }
        if (enemy.charAt(enemy.length()-1) == '_') {
            throw new IllegalArgumentException("Enemy type string cannot have a trailing underscore");
        }
        // does not matter whether the name is mixed, completely upper or lower case,
        // converted to lowercase prior to any manipulation of the string.
        String[] enemyParts = enemy.toLowerCase(Locale.ENGLISH).split("_");
        String label = "";
        for (int i = 0; i < enemyParts.length; i++) {
            if (enemyParts[i].isEmpty()) {
                return;
            }
            char[] word_char_array = enemyParts[i].toCharArray();
            String firstLetter = String.valueOf(word_char_array[0]).toUpperCase(Locale.ENGLISH);
            label = label.concat(firstLetter);

            for (int j = 1; j < word_char_array.length; j++) {
                label = label.concat(String.valueOf(word_char_array[j]));
            }

            label = label.concat(" ");
        }
        label = label.stripTrailing();
        this.enemyLabel = label;
    }

    public String getEnemyLabel() {
        return this.enemyLabel;
    }
}