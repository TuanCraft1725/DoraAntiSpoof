# DoraAntiSpoof 1.0.1

Paper plugin for heuristic detection of suspicious client brand strings and plugin-channel registrations.

## Requirements
- Paper 1.20.1 through 26.2
- Java 17 for Paper 1.20.1; Java 21 for Paper 1.20.5–1.21.11; Java 25 for Paper 26.2
- PacketEvents installed separately (required dependency)
- Build uses Java 21 compiler toolchain and emits Java 17 bytecode.

## Build
Run `gradle clean build`. Output: `build/libs/DoraAntiSpoof-1.0.1.jar`.

## Install
1. Install PacketEvents in the server's `plugins` folder.
2. Install DoraAntiSpoof.
3. Restart the server.
4. Configure `plugins/DoraAntiSpoof/config.yml`.
5. Run `/doraantispoof reload` after changes.

## Commands
- `/doraantispoof info`
- `/doraantispoof reload`

## Permissions
- `doraantispoof.admin`
- `doraantispoof.alerts`

## Important limitations
Client brand and plugin channels are client-provided metadata, not cryptographic identity. A brand string can be forged, and legitimate mods/proxies may register unusual channels. This initial version uses configurable heuristics, defaults to ALERT-only, and does not claim perfect detection. Test with `punishment.mode: ALERT` before enabling KICK. Bedrock exemption is based on the received brand pattern only; install and configure Geyser/Floodgate separately for authoritative Bedrock identification.
