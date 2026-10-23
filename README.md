# SkyLogin

Authentication plugin for the **SkyMinigames** server (Spigot/Paper 1.8.8, Java 8), inspired by nLogin.

## Planned features
- `/register`, `/login`, `/logout`, `/changepassword`, `/unregister`
- `/skylogin` admin commands (reload, forcelogin, unregister, changepassword, info)
- SQLite or MySQL storage (HikariCP connection pool)
- BCrypt / SHA-256 / SHA-512 password hashes (nLogin-compatible formats)
- IP sessions, login timeout, attempt limit, accounts-per-IP limit
- Unauthenticated players are frozen, hidden and can't chat, use commands or interact

## Project layout
| Package | Purpose |
|---|---|
| `api` | Public API and events for other plugins |
| `auth` / `auth.hash` | Login logic and password hashing |
| `command` | Command executors |
| `config` / `message` | `config.yml` and `messages.yml` handling |
| `listener` | Bukkit event listeners (blocking unauthenticated players) |
| `model` | Data classes (account, session) |
| `session` | IP session cache |
| `storage` / `storage.sql` | Database access (SQLite / MySQL) |
| `task` | Scheduled tasks (reminders, timeout) |
| `util` | Helpers |

## Build
```bash
mvn clean package
```
The jar is created in `target/`. In VS Code, run the **Deploy to server** task to build and copy it to the SkyMinigames server's `plugins` folder.
