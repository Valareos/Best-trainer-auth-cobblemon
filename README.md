# Best Profile Auth

> **Formerly:** Best Trainer Auth / `Best-trainer-auth-cobblemon`  
> **Current version:** v0.3.1  
> **Loader:** Fabric only (Minecraft 1.21.1)

A server-side Fabric mod that allows **multiple persistent player profiles per Minecraft account**, designed for shared-computer and lab environments.

Originally built for [Best Disability Support](https://bestdisabilitysupport.com.au) Cobblemon servers, the project now supports **vanilla player data** on any Fabric server, with **optional Cobblemon** integration via a handler architecture.

---

## Maintainer note

**Update this README whenever you change user-facing behaviour, commands, config, or release a new version.**  
Keep `mod_version` in `gradle.properties`, the version section below, and command/config docs in sync.

---

## Features

- Multiple password-protected profiles per server
- Full per-profile swap of inventory, location, health, XP, and ender chest (vanilla player data)
- Cobblemon support: party, PC, and player data (when Cobblemon is installed)
- Handler-based storage (`vanilla`, `cobblemon`) with legacy snapshot compatibility
- Autosave for active profiles on a configurable interval
- Timestamped backups with retention limits before each save
- Existing-server migration (`/profile claimcurrent`)
- Self-registration option (`allowSelfRegistration`)
- Staff admin commands without OP (`/profile staff <password> ...`)
- Dual command namespaces: `/profile` and `/trainer` (identical logic)
- Locks movement and interactions until a profile is selected
- Safe disconnect / reconnect workflow to prevent data loss
- Session isolation: live UUID data cleared after logout so the next user cannot inherit another profile

---

## Requirements

| Requirement | Notes |
|-------------|--------|
| **Fabric Loader** | Required — does not run on Paper, Spigot, Forge, NeoForge, or plain vanilla |
| **Fabric API** | Required |
| **Minecraft** | 1.21.1 |
| **Java** | 21 |
| **Cobblemon** | Optional — vanilla handler works without it |

---

## Commands

Both `/profile` and `/trainer` run the same logic. Examples use `/profile`.

### Player commands

| Command | Description |
|---------|-------------|
| `/profile login <key> <password>` | Select a profile (disconnects and reconnects to load data) |
| `/profile logout` | Save current profile and disconnect |
| `/profile whoami` | Show active profile |
| `/profile register <key> <password>` | Create your own profile (only if `allowSelfRegistration` is enabled) |
| `/profile claimcurrent <key> <password>` | One-time import of existing UUID save into a new profile |

### Admin commands (OP level 4 by default)

| Command | Description |
|---------|-------------|
| `/profile create <key> <password>` | Create a profile |
| `/profile delete <key>` | Delete profile account and snapshot data |
| `/profile setpassword <key> <password>` | Change profile password |
| `/profile setpin <key> <password>` | Alias for `setpassword` |
| `/profile enable <key>` | Re-enable a profile |
| `/profile disable <key>` | Disable a profile |
| `/profile list` | List all profiles |

### Staff commands (no OP required)

When `adminOverridePassword` is set in config, staff can run admin commands with:

```text
/profile staff <staffPassword> <admin-command> ...
```

Examples:

```text
/profile staff MyStaffSecret setpassword bryanb newpass
/profile staff MyStaffSecret list
/profile staff MyStaffSecret create someone secret123
```

The staff password is **not** shown in tab completion. Wrong password returns a generic error.

---

## How it works

1. Player joins → locked until they log into a profile.
2. `/profile login` → pending profile stored → player disconnects.
3. On reconnect → profile snapshot restored into the live UUID save → player can play.
4. On logout / disconnect / autosave → snapshot saved, backup created, live UUID data cleared.

Each profile stores its own snapshot under `config/best-trainer-auth/trainers/<profile>/`.

---

## Profile storage

Profiles are stored at:

```text
config/best-trainer-auth/trainers/<profile>/
```

### Current handler layout (v0.3.x)

```text
snapshot/
  vanilla/
    playerdata.dat
    playerdata.dat_old
  cobblemon/
    cobblemonplayerdata.json
    cobblemonplayerdata.json.old
    party/
    pc/
```

### Legacy layout (v0.2.x, still supported on restore)

```text
snapshot/
  playerdata.dat
  cobblemonplayerdata.json
  party/
  pc/
```

Backups:

```text
config/best-trainer-auth/trainers/<profile>/backups/<YYYY-MM-DD_HH-mm-ss>/
```

Migration markers:

```text
config/best-trainer-auth/migration-markers.json
```

Account registry:

```text
config/best-trainer-auth/accounts.json
```

**Internal paths remain `best-trainer-auth` for backward compatibility** even though the project is branded Best Profile Auth.

---

## Configuration

Created on first run at:

```text
config/best-trainer-auth/config.json
```

New keys are merged automatically when the mod upgrades `configVersion`.

### Example (v4)

```json
{
  "configVersion": 4,
  "adminBypassPermissionLevel": 4,
  "lockMovement": true,
  "lockInteractions": true,
  "lockBlockBreaking": true,
  "lockCombat": true,
  "autosaveIntervalTicks": 6000,
  "maxBackupsPerProfile": 10,
  "allowSelfRegistration": false,
  "adminOverridePassword": ""
}
```

| Option | Default | Description |
|--------|---------|-------------|
| `adminBypassPermissionLevel` | `4` | OP level required for admin `/profile` commands |
| `lockMovement` | `true` | Freeze unauthenticated players |
| `lockInteractions` | `true` | Block use/attack while locked |
| `lockBlockBreaking` | `true` | Block breaking while locked |
| `lockCombat` | `true` | Block damage while locked |
| `autosaveIntervalTicks` | `6000` | Autosave interval (~5 min at 20 TPS; `0` = disabled) |
| `maxBackupsPerProfile` | `10` | Backups kept per profile |
| `allowSelfRegistration` | `false` | Allow `/profile register` |
| `adminOverridePassword` | `""` | Enables `/profile staff ...` when non-empty |

---

## Build

Requires **JDK 21**.

```bash
chmod +x gradlew
./gradlew build
```

Output:

```text
build/libs/best-profile-auth-<version>.jar
```

Version is set in `gradle.properties` (`mod_version`).

---

## Install

1. Copy `best-profile-auth-<version>.jar` into your Fabric server `mods/` folder.
2. Ensure **Fabric API** is installed.
3. Start the server — config and folders are created automatically.
4. Create profiles (admin) or enable self-registration in config.

### First-time staff setup

```text
/profile create BryanGB secretpass
/profile create AlexM otherpass
```

Players:

```text
/profile login BryanGB secretpass
```

---

## Existing server migration

For players who already have progress in the normal UUID save before profiles existed:

```text
/profile claimcurrent <key> <password>
```

- Copies live UUID data into a new profile snapshot
- Marks the Minecraft account as migrated (one-time per UUID)
- Disconnects for reconnect into the new profile

---

## Safety and data ownership

- Profile switching uses **disconnect / reconnect** to load data safely.
- Autosave and logout create **timestamped backups** before overwriting snapshots.
- After logout, **live UUID world files are cleared** so the next person on a shared PC cannot inherit the previous session.
- Once deployed, player progress may live primarily in **profile snapshots**, not only in `world/playerdata/`.

Removing the mod without exporting profile data back to standard UUID saves may make profile progress inaccessible.

---

## Project layout

| Component | Role |
|-----------|------|
| `BestTrainerAuthMod` | Main initializer, events, autosave |
| `TrainerCommand` | `/profile` and `/trainer` commands |
| `TrainerBridge` | Profile save / restore orchestration |
| `ProfileDataHandler` | Handler interface |
| `VanillaPlayerDataHandler` | Inventory, location, XP, health, etc. |
| `CobblemonDataHandler` | Party, PC, Cobblemon player data |
| `TrainerStore` | Profile account registry (`accounts.json`) |
| `MigrationStore` | One-time migration markers |
| `TrainerSessionService` | Per-connection login state |
| `LockService` | Keeps unauthenticated players frozen |

---

## Compatibility

| Environment | Support |
|-------------|---------|
| Fabric 1.21.1 | Full |
| Fabric + Cobblemon | Full |
| Fabric without Cobblemon | Vanilla profiles only |
| Paper / Spigot / Forge / NeoForge | Not supported (planned future ports) |

---

## Version history

### v0.3.1

- Self-registration (`allowSelfRegistration`, `/profile register`)
- Staff admin override (`adminOverridePassword`, `/profile staff ...`)
- Session isolation fix (wipe live UUID after save; stale session cleanup)
- Backup logging improvements
- JAR name: `best-profile-auth`

### v0.3.0

- Handler architecture (`ProfileDataHandler`, vanilla + Cobblemon handlers)
- Legacy snapshot restore compatibility
- Migration system (`claimcurrent`, migration markers)
- Autosave + backup retention
- Dual `/profile` and `/trainer` commands
- Cobblemon restore parity with v0.2.0

### v0.2.0

- Production-ready profile swap (inventory, location, Cobblemon)
- Autosave, backups, profile command alias

---

## License

MIT — see [LICENSE](LICENSE).

Attribution appreciated:

**Best Disability Support**  
https://bestdisabilitysupport.com.au
