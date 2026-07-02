# Dead Drop — Location-Based BLE Messaging
[![Release](https://img.shields.io/github/v/release/chotathor/DeadDrop)](https://github.com/chotathor/DeadDrop/releases)
[![License](https://img.shields.io/github/license/chotathor/DeadDrop)](LICENSE)
[![Stars](https://img.shields.io/github/stars/chotathor/DeadDrop)](https://github.com/chotathor/DeadDrop/stargazers)

> Leave messages at physical locations. Anyone nearby picks them up. Offline-first.

## Download
**[Get the APK](https://github.com/chotathor/DeadDrop/releases)**

## Features
- GPS geo-tagged message drops
- BLE mesh for offline discovery
- WebSocket sync for global delivery
- Threaded replies & upvotes
- Proximity map (signal strength radar)
- Favorite devices with auto-alert
- Export scan reports

## Architecture
BLE discovers nearby users → messages sync via WebSocket to VPS → global delivery. Offline? Messages cache locally and sync when reconnected.

## WebSocket Server


## License
MIT