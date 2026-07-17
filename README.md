# Funny Effects

A Minecraft NeoForge mod adding 31 fun, powerful, and weird items.

## Categories

| Category | Color | Items |
|---|---|---|
| 🔴 Combat | Red | Lifesteal Blade, Thorns Shield, Thunder Sword, Yeeter Hammer, Disco Sword, Slapfish |
| 🟡 Tools | Gold | Smelter Pick, Gravity Pickaxe, Replanter Hoe, Treecapitator |
| 🟣 Armor | Purple | Potato Goggles, Rainbow Boots, Bouncy Boots, Sneaky Helmet, Lava Walker |
| 🔵 Gadgets | Blue | Magnetic Glove, Squeaky Toy, XP Magnet, Void Pearl, Gravity Anchor, Party Popper, Infinite Pearl, **Mystery Box**, **Void Pearl Trap** |
| 🩷 Interaction | Pink | Chicken Wand, Dinnerbone Bat, Mob Catcher |
| 🟢 Food | Green | Bouncy Slime, Flatulent Bean |

## Highlights
- **Lifesteal Blade**: Heals 20% of damage dealt (+10% with Thorns Shield offhand — 30% total)
- **Smelter Pick**: Auto-smelts ores
- **Treecapitator**: Breaks entire trees at once
- **Gravity Anchor**: Float for 5s + launch mobs (15s cooldown)
- **Dinnerbone Bat**: Flips entities upside-down
- **Mob Catcher**: Captures any living mob — release to make it an invincible pet that follows you for 5 minutes
- **Lava Walker**: Turns lava to obsidian while sprinting
- **Infinite Pearl**: Never consumed, 10% Endermite spawn
- **Mystery Box**: Craftable (8 iron + 1 diamond), gives random loot from `#funnyeffects:mystery_box_loot` tag
- **Void Pearl Trap**: Right-click a player to teleport them 20 blocks in your look direction

## New Items

### Mystery Box
Craftable with 8 iron ingots surrounding a diamond. On use, grants a random item from the `#funnyeffects:mystery_box_loot` tag. Single-use, consumed on activation.

### Void Pearl Trap
Right-click another player to teleport them 20 blocks away in your look direction. Plays ender pearl sound. Single-use, consumed on throw.

### Item Combos
- **Lifesteal Blade + Thorns Shield**: lifesteal increases from 20% to 30% when Thorns Shield is held in offhand

### Mob Pet System
When releasing a captured mob from the Mob Catcher, the entity becomes:
- Invincible (Resistance 255 for 5 minutes)
- Persistent (won't despawn naturally)
- Follows the owner (teleports within 3 blocks every second)
- Despawns automatically after 5 minutes

## Cooldowns
Items with cooldowns (1.5s): Squeaky Toy, Void Pearl, Party Popper, Disco Sword, Slapfish, Chicken Wand, Dinnerbone Bat
- Infinite Pearl: 1s
- Gravity Anchor: 15s

## KubeJS Compatibility
- **6 item tags** (`data/funnyeffects/tags/item/`) — reference items by `#funnyeffects:funny_combat`, etc.

Example KubeJS script:
```javascript
ServerEvents.tags('item', event => {
  event.add('funnyeffects:funny_gadgets', 'minecraft:fishing_rod')
})
```

## Random Events
The mod registers fun random events via core-lib's RandomEventManager:
- Mysterious giggles
- "Did something move?!"
- Chicken invasions
- Random weather changes

## Dependencies
- **core-lib** (required) — shared library
- Minecraft 1.21.1, NeoForge

## 🎛️ BikiniConfig Options
Accessible via `/bn` in-game: Combat Items, Gadget Items, Armor Items (toggle each category).

## 🎛️ BikiniConfig Options
Accessible via `/bn`: Combat, Gadgets, Armor toggles.
