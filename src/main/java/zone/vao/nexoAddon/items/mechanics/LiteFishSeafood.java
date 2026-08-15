package zone.vao.nexoAddon.items.mechanics;

/** Configuration for a temporary LiteFish buff granted by consuming a Nexo item. */
public record LiteFishSeafood(
    String name,
    double durationSeconds,
    double bonusCatchChance,
    int gameSpeed,
    int gameSize,
    int playerHealth,
    int dropHealth) {}
