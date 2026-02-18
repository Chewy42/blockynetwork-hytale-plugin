# BlockyNetwork Hytale Plugin

Official Java companion plugin for BlockyNetworks.

This plugin connects your Hytale server to BlockyNetworks for account linking, server linking, and real-time player presence updates.

## Features

- Player account linking via `/link`
- Server linking via `/link server`
- Automatic heartbeat updates to BlockyNetworks
- Lightweight config file with secure server secret storage

## Requirements

- Java 21
- Hytale server API jar (`HytaleServer.jar`)

## Setup

1. Place your Hytale server API jar at:
   - `libs/HytaleServer.jar`
2. Build the plugin:

```bash
./gradlew clean shadowJar
```

3. Copy the built jar from:
   - `build/libs/BlockyNetwork-1.0.1.jar`
4. Put it in your server's `mods/` directory.
5. Start the server once to generate config, then edit `blockynetwork.json`.

## Configuration

The plugin reads config from:

- `plugins/BlockyNetwork/blockynetwork.json`

Expected keys:

```json
{
  "convexHttpUrl": "https://<your-blockynetworks-backend>",
  "serverId": "",
  "serverSecret": "",
  "serverName": "My Hytale Server"
}
```

## Commands

- `/link` -> Generate a player link code
- `/link server` -> Generate server link code (requires permission `blockynetwork.linkserver`)

## Release Downloads

Use GitHub Releases for ready-to-use `.jar` files.
