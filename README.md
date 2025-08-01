# EzDuels

A comprehensive dueling plugin for Paper 1.21+ servers that provides a custom dueling system with inventory-based combat, betting mechanics, arena management, and advanced features.
## Known Bugs
- Put players in adventure mode during countdown
- Set to full health / hunger before fight
- Implement tab-completer for /duel
- If a player logs out during a duel, and it is a cracked server with LibreLogin installed, conflicts may occur
leading to a) either insecurities to player log-off position or b) the player being stuck in the arena.
## Features

- **Custom Duel System**: Players duel using their current inventory (no kits)
- **Betting System**: Interactive GUI for wagering items between players
- **Arena Management**: WorldEdit integration for creating and managing duel arenas
- **Prize System**: Virtual inventory for overflow items with expiration
- **Statistics Tracking**: Win/loss records for all players
- **Spectator Mode**: Watch ongoing duels in spectator mode
- **Modern API**: Built with Adventure API and modern Paper methods

## Requirements

- **Server**: Paper 1.21+ (or compatible fork)
- **Dependencies**: WorldEdit 7.3.0+
- **Java**: Java 21+

## Installation

1. Download the latest release of EzDuels
2. Place the `.jar` file in your server's `plugins` folder
3. Ensure WorldEdit is installed and working
4. Start/restart your server
5. Configure the plugin using the generated `config.yml`

## Quick Start Guide

### For Players

#### Starting a Duel
1. Run `/duel <playername>` to challenge someone
2. Configure your duel settings in the GUI:
   - **Keep Inventory**: Toggle whether items are dropped on death
   - **Betting**: Enable/disable item wagering
   - **Arena**: Choose a specific arena or use auto-selection
3. Click "Confirm Duel" to send the challenge
4. The target player receives a clickable message to accept with `/duelaccept`

#### Betting System
If betting is enabled:
1. After both players accept, a betting GUI opens automatically
2. Place items you want to wager in your side (left 4 columns for challenger, right 4 columns for target)
3. Click your confirmation button (bottom row) when ready
4. Both players must confirm before the duel begins
5. Use `/betmenu` to reopen the betting interface if closed

#### During the Duel
- A 30-second countdown begins (shown in action bar)
- Both players can `/skip` to start immediately
- Fight using your current inventory and items
- You can place/break blocks, but only ones you placed yourself
- Use `/leavefight` to forfeit (you'll lose items/bets if applicable)

#### Prize System
- If your inventory is full when winning items, they go to your prize collection
- Use `/prizes` to view and claim items
- Prizes expire after 1 hour, so claim them quickly!

### For Administrators

#### Setting Up Arenas

1. **Create an Arena Group**:
   ```
   /duelsadmin arena create plains
   ```

2. **Define Arena Boundaries**:
   - Use WorldEdit to select the arena area (`//wand`, `//pos1`, `//pos2`)
   - Run `/duelsadmin arena define plains`
   - This creates `plains1` (additional arenas become `plains2`, `plains3`, etc.)

3. **Set Spawn Points**:
   - Stand where Player 1 should spawn: `/duelsadmin arena spawnloc 1`
   - Stand where Player 2 should spawn: `/duelsadmin arena spawnloc 2`

4. **Create Multiple Arenas**:
   - Repeat steps 2-3 to create `plains2`, `plains3`, etc.
   - Players will be assigned to available arenas automatically

#### Arena Management Commands
- `/duelsadmin arena list` - View all created arenas
- `/duelsadmin reload` - Reload plugin configuration

## Commands

### Player Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/duel <player>` | Challenge a player to a duel | `ezduels.duel` |
| `/duelaccept` | Accept a pending duel challenge | `ezduels.duel` |
| `/betmenu` | Open/reopen the betting interface | `ezduels.bet` |
| `/skip` | Vote to skip the countdown | `ezduels.duel` |
| `/leavefight` | Forfeit the current duel | `ezduels.duel` |
| `/cancelfight` | Cancel a duel before it starts | `ezduels.duel` |
| `/prizes` | View and claim prize items | `ezduels.prizes` |
| `/spectatefight` | Spectate an ongoing duel | `ezduels.spectate` |

### Admin Commands
| Command | Description | Permission |
|---------|-------------|------------|
| `/duelsadmin arena create <name>` | Create a new arena group | `ezduels.admin` |
| `/duelsadmin arena define <name>` | Define arena from WorldEdit selection | `ezduels.admin` |
| `/duelsadmin arena spawnloc <1\|2>` | Set spawn locations for arena | `ezduels.admin` |
| `/duelsadmin arena list` | List all created arenas | `ezduels.admin` |
| `/duelsadmin reload` | Reload plugin configuration | `ezduels.admin` |

## Permissions

### Permission Groups
- `ezduels.*` - All permissions (for admins)
- `ezduels.duel` - Basic dueling (default: true)
- `ezduels.bet` - Betting system (default: true)
- `ezduels.prizes` - Prize system (default: true)
- `ezduels.spectate` - Spectate duels (default: true)
- `ezduels.admin` - Admin commands (default: op)

## Configuration

The `config.yml` file allows you to customize various aspects of the plugin:

```yaml
# EzDuels Configuration
plugin:
  prefix: "<gradient:#FF6B6B:#4ECDC4>[EzDuels]</gradient>"
  
duels:
  countdown-duration: 30 # seconds
  bet-menu-duration: 300 # seconds (5 minutes)
  bet-reminder-interval: 5 # seconds
  reconnect-grace-period: 120 # seconds (2 minutes)
  
prizes:
  expiration-time: 3600 # seconds (1 hour)
  reminder-interval: 300 # seconds (5 minutes)
  
arenas:
  default-world: "world"
  dedicated-world: "duels_world" # optional
```

### Message Customization
All messages support MiniMessage formatting and can be customized in the config. Messages include click and hover events for better user experience.

## Data Storage

The plugin stores data in YAML files:
- `arenas.yml` - Arena definitions and spawn points
- `stats.yml` - Player statistics (wins/losses)
- Prize data is stored in memory and expires automatically

## Duel Mechanics

### Disconnection Handling
- **Keep Inventory ON + No Betting**: Instant loss for disconnected player
- **Keep Inventory OFF or Betting Enabled**: 2-minute grace period to reconnect

### Arena Restrictions
- Players can only place/break blocks they placed themselves
- Cannot break original arena blocks
- Combat is confined to arena boundaries

### Betting Rules
- Items are removed from inventory when both players confirm
- Winner receives all wagered items
- If inventory is full, items go to prize system
- Betting GUI times out after 5 minutes

## Troubleshooting

### Common Issues

**"Failed to create arena" error**:
- Ensure you have a WorldEdit selection active
- Make sure you have permission to use WorldEdit
- Verify the selection is in a loaded world

**Betting GUI not opening**:
- Check that betting is enabled for the duel
- Ensure both players have accepted the duel
- Verify players have `ezduels.bet` permission

**Arena spawn points not working**:
- Make sure you're standing inside the arena when setting spawn points
- Verify both spawn points (1 and 2) are set
- Check that the arena was properly defined with WorldEdit

### Performance Considerations
- The plugin uses async operations where possible
- Arena data is cached in memory for fast access
- Statistics are saved periodically to prevent data loss

## Support

For issues, feature requests, or contributions:
- Check the documentation above
- Review the configuration options
- Ensure all dependencies are properly installed
- Verify permissions are correctly set
