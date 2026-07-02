# Dead Drop — Location-Based BLE Messaging
[![Stars](https://img.shields.io/github/stars/chotathor/DeadDrop)](https://github.com/chotathor/DeadDrop/stargazers)

> Leave messages at physical locations. Pick up messages left by others. Offline, anonymous, free.

## Features
- BLE mesh network — no internet needed
- GPS geo-tagged messages
- Auto-discovery of nearby devices
- Threaded replies & upvotes
- Proximity map (signal strength radar)
- Favorite devices with auto-alert
- Export scan reports

## How It Works
Drop a message at your location. Anyone passing by with the app picks it up via BLE. Messages propagate through the mesh network. Optional WebSocket sync for global delivery via VPS.

## WebSocket Server
The backend sync server runs on port 8081. Deploy with:


## Tech Stack
- Kotlin Android
- BLE GATT server + advertising
- OkHTTP WebSocket client
- Python WebSocket server

## License
MIT