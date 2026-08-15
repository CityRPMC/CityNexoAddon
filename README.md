# NexoAddon
Backward compability and new features for Nexo plugin!

[![CodeFactor](https://www.codefactor.io/repository/github/naimadx123/nexoaddon/badge)](https://www.codefactor.io/repository/github/naimadx123/nexoaddon)

---

### 📕 [Documentation](https://nexoaddon.gitbook.io/nexoaddon-docs)

### 📥 Download
- [Polymart](https://polymart.org/r/6950)
- [Spigot](https://www.spigotmc.org/resources/nexoaddon.121241)

### 📞 Contact
- [Discord](https://discord.com/invite/aSRYxqSjVJ)
---
### ✍ Description
NexoAddon is a powerful expansion for the Nexo plugin, bringing high-performance gameplay mechanics, block behaviors, and data-driven customization to your Minecraft server using Nexo-managed assets.

Designed as a feature-rich addon for servers using Nexo, NexoAddon enhances your content with intuitive mechanics.

### ✨ Core Features:

🔧 **Block & Item Mechanics**
* Aura / BlockAura – Area-based effects for entities or surrounding blocks.
* BigMining – Mine multiple blocks with one action.
* BedrockBreak – Add logic to break normally unbreakable blocks.
* BottledExp – Bottle and retrieve experience.
* Decay – Leaves-style decay for CustomBlocks.
* Infested – Silverfish-like infested block behavior.
* KillMessage – Custom messages on entity death.
* MiningTools – Restrict breaking to specific tools.
* Repair – Custom repair logic using defined items.
* ShiftBlock – Swap blocks & furniture.
* Signal – Redstone-style logic system for CustomBlocks.
* SpawnerBreak – Custom spawner-breaking conditions.
* Stackable / Unstackable – Toggle stacking on items.

⚙️ **Utility Components**
* equippable – Control equippable item logic.
* fertilizer – Trigger effects using bonemeal.
* jukebox_playable – Play custom items in jukeboxes.
* skull_value – Register & handle custom skulls.

🗿 **Totem Animation**
* Play custom totem animation

🌱 **World Populators**
* Modify terrain generation with ores, decorations, or block overrides.

⚒️ **Crafting Enhancements**
* Smithing Recipe Support.

🆕 **And more coming soon...**

🛠️ Built for Performance:
* Lightweight, optimized, and fully modular.
* Easily configurable via `config.yml`.
* Seamless compatibility with Nexo item system.

### 📥 **Downloads & Docs:**  
Available on [Polymart/voxel.shop](<https://voxel.shop/product/6950/nexoaddon>), [Spigot](<https://www.spigotmc.org/resources/nexoaddon.121241/>), and [GitHub](<https://github.com/Naimadx123/NexoAddon>).  
[Join our Discord](https://discord.gg/aSRYxqSjVJ) for support, tutorials, and feature previews!
[Documentation](<https://nexoaddon.gitbook.io/docs>)

🔗 Expand your Nexo-powered server with deep custom interactions using **NexoAddon**.

### LiteFarm harvesting

The CityRP fork can harvest mature LiteFarm crops with a separate item mechanic. LiteFarm still handles crop permissions, rewards, events, and its global replant setting.

```yaml
Mechanics:
  litefarm_harvesting:
    cooldown: 10 # seconds
    radius: 5
    height: 3
    lower_item_durability: true
```

Do not add Nexo's regular `harvesting` mechanic to the same item unless it should also harvest vanilla crops.

### LiteFish seafood buffs

The CityRP chef foods integrate with LiteFish without requiring changes to their Nexo item files. Eating one seafood food replaces the player's current seafood buff:

* `tmls_fish_sandwich` — Angler's Focus makes the fishing minigame easier for five minutes.
* `tmls_tuna_salad_sandwich` — Bountiful Catch gives a 10% chance to duplicate a successful catch for five minutes.
* `tmls_fish_stew` — Captain's Feast combines the easier minigame with a 15% bonus-catch chance for five minutes.

The item IDs, durations, minigame modifiers, chances, and action-bar messages are configurable under `litefish_seafood` in `config.yml`. Chances use decimal values from `0.0` to `1.0`. LiteFish is optional; CityNexoAddon continues loading when it is absent.

### WeaponMechanics repairs

WeaponMechanics 4.x can store custom durability on each gun. A Nexo repair item can target those guns by their WeaponMechanics title, including `*` and `?` wildcards:

```yaml
gun_repair_kit:
  material: PAPER
  Mechanics:
    weaponmechanics_repair:
      fixed_amount: 250
      whitelist:
        - "AR_*"
        - "SMG_*"
      blacklist:
        - "*_Prototype"
```

`weaponmechanics_repair` is a dedicated CityNexoAddon mechanic, parallel to mechanics such as `litefarm_harvesting`; it is not part of the generic `repair` mechanic. Use `"*"` to repair every WeaponMechanics weapon. The WeaponMechanics weapon itself must configure `Info.Weapon_Item.Durability.Max_Damage`; `Shoot.Durability_Per_Shot` controls wear, and blacklists take precedence over whitelists.

Legacy guns are upgraded in place when players join, open inventories, pick up items, or interact with a weapon. The migration preserves the complete item and only adds the configured max durability and removes the legacy unbreakable flag. Run `/nexoaddon migratewmdurability` after installing or reloading the weapon configs to sweep every online player's inventory and ender chest immediately. Guns belonging to offline players migrate when they next join, while guns in containers migrate when that container is opened. Set `weaponmechanics.auto_migrate_durability: false` in `config.yml` to disable lazy migration.

---
### 👥 Contributors
<a href="https://github.com/Naimadx123/NexoAddon/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=Naimadx123/NexoAddon&max=30" alt="Contributors"/>
</a>
