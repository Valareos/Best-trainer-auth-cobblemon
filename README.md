# Best Trainer Auth

A Fabric server-side trainer login system designed for Cobblemon servers and shared-computer environments.

This mod allows multiple persistent **trainer characters** to exist independently of the Minecraft account used to connect to the server.

Originally developed for **Best Disability Support** to allow participants using shared computers to maintain their own characters without needing their own Minecraft accounts.

https://bestdisabilitysupport.com.au

---

## Key Concept

Most Minecraft servers identify players by **UUID**, which ties a player to a specific Minecraft account.

Best Trainer Auth instead treats the Minecraft account as **transport only**.

A player logs in using a **Trainer Key + PIN**, which loads that trainer's saved state.

Each trainer has their own:

- Inventory
- Location
- Cobblemon party
- Cobblemon PC storage
- Vanilla player data

This allows persistent characters even when players use different computers or Minecraft accounts.

---

## Features

- Multiple trainer identities per server
- Trainer authentication using PIN codes
- Automatic swapping of player save data
- Compatible with Cobblemon
- Works behind Velocity proxy setups
- Designed for shared computer environments
- Snapshot-based player data system

---

## Commands

### Player Commands

/trainer login <key> <pin>  
/trainer logout  
/trainer whoami  

### Admin Commands

/trainer create <key> <pin>  
/trainer delete <key>  
/trainer setpin <key> <pin>  
/trainer enable <key>  
/trainer disable <key>  
/trainer list  

---

## How It Works

Instead of changing player UUIDs, this mod swaps the underlying player data files before login.

When a trainer logs in:

1. The trainer key is authenticated.
2. The player reconnects.
3. The server loads the trainer's saved snapshot.
4. Minecraft loads the player normally using that snapshot.

When a trainer logs out:

1. Current player data is saved as a trainer snapshot.
2. The player reconnects.
3. They may log in as a different trainer.

This ensures data isolation between trainers.

---

## Intended Use Cases

- Shared computer environments
- Disability support centres
- Schools and training labs
- LAN events
- Cobblemon servers
- Roleplay servers with multiple characters

---

## Requirements

- Minecraft 1.21.1
- Fabric Loader
- Fabric API
- Cobblemon

---

## Compatibility

Developed and tested with:

- Fabric
- Cobblemon
- Velocity proxy environments

Other modpacks may work but have not been extensively tested.

---

## Building

Run:

./gradlew build

The compiled mod will appear in:

build/libs/

---

## Known Limitations

- Trainer switching currently requires a reconnect.
- Designed primarily for Cobblemon environments.
- Large public servers may wish to extend command functionality.

---

## Credits

Developed by **Best Disability Support**.

https://bestdisabilitysupport.com.au

This mod was created to support inclusive gaming environments and shared computer access for participants.

---

## Contributing

Pull requests and improvements are welcome.

If you build improvements or extensions, please ensure attribution to **Best Disability Support** remains intact as required by the license.

---

## License

Licensed under the **Apache License 2.0**.

Attribution to **Best Disability Support (https://bestdisabilitysupport.com.au)** must be preserved in derivative works.

See the LICENSE file for full details.
