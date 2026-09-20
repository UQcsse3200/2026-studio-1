package com.csse3200.game.components.npc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;
import org.junit.jupiter.api.Test;

class NameGeneratorTest {

  @Test
  void shouldGenerateName() {
    NameGenerator generator = new NameGenerator(new Random(1));

    String name = generator.generateName();

    assertNotNull(name);
    assertTrue(name.length() > 0);
  }

  @Test
  void shouldGenerateNamesWithExpectedFormat() {
    NameGenerator generator = new NameGenerator(new Random(1));

    String name = generator.generateName();

    assertTrue(name.matches("[A-Z][a-z]+"));
  }

  @Test
  void shouldRejectNullRandom() {
    assertThrows(IllegalArgumentException.class, () -> new NameGenerator(null));
  }

  @Test
  void shouldGenerateDifferentNames() {
    NameGenerator generator = new NameGenerator();

    String firstName = generator.generateName();
    String secondName = generator.generateName();

    assertNotNull(firstName);
    assertNotNull(secondName);
  }
}
