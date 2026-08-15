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

### WeaponMechanics repairs

WeaponMechanics 4.x can store custom durability on each gun. A Nexo repair item can target those guns by their WeaponMechanics title, including `*` and `?` wildcards:

```yaml
gun_repair_kit:
  material: PAPER
  Mechanics:
    repair:
      fixed_amount: 250
      weaponmechanics:
        whitelist:
          - "AR_*"
          - "SMG_*"
        blacklist:
          - "*_Prototype"
```

Use `"*"` to repair every WeaponMechanics weapon. The WeaponMechanics weapon itself must configure `Info.Weapon_Item.Durability.Max_Damage`; `Shoot.Durability_Per_Shot` controls wear. Generic material/Nexo allowlists and the WeaponMechanics allowlist are additive, while all blacklists take precedence.

---
### 👥 Contributors
<a href="https://github.com/Naimadx123/NexoAddon/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=Naimadx123/NexoAddon&max=30" alt="Contributors"/>
</a>
