# Funny Effects

A Minecraft NeoForge mod adding 27 fun, powerful, and weird items.

## Categories

| Category | Color | Items |
|---|---|---|
| 🔴 Combat | Red | Lifesteal Blade, Thorns Shield, Thunder Sword, Yeeter Hammer, Disco Sword, Slapfish |
| 🟡 Tools | Gold | Smelter Pick, Gravity Pickaxe, Replanter Hoe, Treecapitator |
| 🟣 Armor | Purple | Potato Goggles, Rainbow Boots, Bouncy Boots, Sneaky Helmet, Lava Walker |
| 🔵 Gadgets | Blue | Magnetic Glove, Squeaky Toy, XP Magnet, Void Pearl, Gravity Anchor, Party Popper, Infinite Pearl |
| 🩷 Interaction | Pink | Chicken Wand, Dinnerbone Bat, Mob Catcher |
| 🟢 Food | Green | Bouncy Slime, Flatulent Bean |

## Highlights
- **Lifesteal Blade**: Heals 20% of damage dealt
- **Smelter Pick**: Auto-smelts ores
- **Treecapitator**: Breaks entire trees at once
- **Gravity Anchor**: Float for 5s + launch mobs (15s cooldown)
- **Dinnerbone Bat**: Flips entities upside-down
- **Mob Catcher**: Captures and releases any living mob
- **Lava Walker**: Turns lava to obsidian while sprinting
- **Infinite Pearl**: Never consumed, 10% Endermite spawn

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
