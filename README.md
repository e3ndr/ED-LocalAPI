# ED-LocalAPI

This application acts as a bridge between Elite Dangerous' file-based logs and web apps that want to integrate with the game.

## Used by
Nobody yet! Create an issue if you make a tool that integrates with EDLA :^)

## For users

### Installation
1. Grab the Windows.zip from the [latest release](https://github.com/e3ndr/ED-LocalAPI/releases/latest).
2. Put it somewhere safe where you won't accidentally delete it.
3. Double-click EDLA.exe to start the app!

If you want to close it, head to your tray and right click the icon and select Exit.

## For developers
By default, EDLA listens on `http://localhost:10986` and won't allow connections from other computers on your network. While this _can_ be changed, web-security only allow insecure connections to localhost, so that option is not supported by me. 


### API

**An important note:** EDLA makes no attempt to provide a high-level API for getting data from Elite Dangerous, it just reads the files and streams you any changes in real-time. Refer to [this](https://edcodex.info/?m=doc) excellent guide on how to understand the data.

**Another important note:** EDLA disallows access to files when the game is closed (this is to prevent you from getting stale data). Use the `/game` endpoints to determine when the game is open, at which point you can start streaming data from files.

#### GET /edla/challenge/:toEcho
This endpoint allows you to challenge the endpoint to make sure you're talking to EDLA and that it's alive. This endpoint will return the `toEcho` parameter in plain-text. So if you hit `/edla/challenge/abc123` then it _should_ return `abc123`.

#### GET /game
Returns the game's current state.
```json
{
    "isGameRunning": true
}
```

#### WebSocket /game
Stream's the game's current state and notifies you of changes in real-time.
```json
{
    "isGameRunning": true
}
```

#### GET /files
Lists all files the game is currently writing to.

### GET /file/:filename
Returns the contents of the given file. For the current Journal there's a shortcut called `Journal` which you can use like so: `/file/Journal`. 

Response will be `200 OK` and json-formatted if the game is open or `424 Failed Dependency` and will contain a plain-text error message if the game is not open. Use `/game` to determine when you can read the file.

### WebSocket /file/:filename
Streams the content changes of the given file. 
Every WebSocket message you receive will be the entire file __unless__ you are accessing the Journal, in which case it will only be a new Journal line. If you need historical data from the Journal, use the `GET` endpoint.

Your WebSocket connection will fail if the game is not currently open. Additionally, the WebSocket connection will be closed when the game closes. Use `/game` to determine when you can start streaming changes.