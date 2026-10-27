# Clutch backup & sync API (PHP + MySQL)

Small JSON API that lets the Clutch app back up a farmer's records and restore them on a new phone.
Plain PHP 8.1+ with PDO — no framework, no Composer.

## Endpoints

| Endpoint | Method | Purpose |
|---|---|---|
| `api/register.php` | POST | Create an account with a Ghana phone number, name and password |
| `api/login.php` | POST | Sign in (5 wrong tries per number → 15-minute pause) |
| `api/logout.php` | POST | Sign this phone out (token revoked) |
| `api/me.php` | GET | Who am I + how many records are backed up |
| `api/sync.php` | POST | Push this phone's changes and pull everything newer, in one call |
| `api/delete_account.php` | POST | Delete the account and its backup (password required; Google Play rule) |
| `api/health.php` | GET | Open in a browser after deploying: checks PHP, database and tables |

Signed-in calls send `Authorization: Bearer <token>` and the same token as `X-Clutch-Token`
(many cPanel hosts drop `Authorization`). Tokens are random 64-hex strings; only their SHA-256 is stored.

### How sync works
- Each row keeps the same id on every phone and on the server.
- **Last write wins** on `updatedAt` (when the farmer made the change). Deletes are rows with `deleted: true`, so they sync too.
- The server stamps every stored row with `server_updated_at`; phones pull "everything after my cursor", page by page (`more: true`).
- A push older than the server copy is ignored and the server copy is sent back.

## Deploy

- **Namecheap / cPanel:** follow [DEPLOY-CPANEL.md](DEPLOY-CPANEL.md) step by step.
- Any other host: PHP 8.1+ with `pdo_mysql`, MySQL 5.7+ / MariaDB 10.3+, HTTPS. Import `schema.sql` into an
  existing database, create `config.local.php` from `config.local.php.example`, and serve only the `api/` folder.

## Run it locally (XAMPP)

1. Start **Apache** and **MySQL** in the XAMPP control panel.
2. Create the database and tables (Command Prompt, in the project folder):
   ```
   C:\xampp\mysql\bin\mysql.exe -u root -e "CREATE DATABASE IF NOT EXISTS clutch CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"
   C:\xampp\mysql\bin\mysql.exe -u root clutch < backend\schema.sql
   ```
3. Open `http://localhost/clutch/backend/api/health.php` — it should say `"ok": true`.
4. Run the end-to-end test (creates a throwaway account and deletes it through the API):
   ```
   C:\xampp\php\php.exe backend\tests\api_test.php
   ```

### Testing with a real phone on the same Wi-Fi
- Put this computer's address in the project's `local.properties`:
  `clutch.apiUrl=http://192.168.1.97/clutch/backend/api` (use your own IP from `ipconfig`), then rebuild the app.
- Debug builds also have a **Server address** field on the Backup screen.
- If the phone cannot connect, allow **Apache HTTP Server** through Windows Defender Firewall (Private networks).

## Security notes
- Passwords: `password_hash()` (bcrypt), re-hashed automatically when PHP's default changes.
- All SQL uses prepared statements; every synced field is type- and length-checked.
- Each account only ever reads and writes its own rows (`user_id` is part of every key).
- Not included yet: password reset by SMS, sharing one farm between several accounts.
