# Clutch backup & sync API (PHP + MySQL)

Small JSON API that lets the Clutch app back up a farmer's records and restore them on a new phone.
Plain PHP 8.1+ with PDO — no framework, no Composer.

## What it does

| Endpoint | Method | Purpose |
|---|---|---|
| `api/register.php` | POST | Create an account with a Ghana phone number, name and password |
| `api/login.php` | POST | Sign in (5 wrong tries per number → 15-minute pause) |
| `api/logout.php` | POST | Sign this phone out (token revoked) |
| `api/me.php` | GET | Who am I + how many records are backed up |
| `api/sync.php` | POST | Push this phone's changes and pull everything newer, in one call |

Signed-in calls send `Authorization: Bearer <token>`. Tokens are random 64-hex strings; only their SHA-256 is stored.

### How sync works
- Each row keeps the same id on every phone and on the server.
- **Last write wins** on `updatedAt` (when the farmer made the change). Deletes are rows with `deleted: true`, so they sync too.
- The server stamps every stored row with `server_updated_at`; phones pull "everything after my cursor", page by page (`more: true`).
- A push older than the server copy is ignored and the server copy is sent back.

## Run it locally (XAMPP)

1. Start **Apache** and **MySQL** in the XAMPP control panel.
2. Create the database:
   ```
   C:\xampp\mysql\bin\mysql.exe -u root < backend\schema.sql
   ```
3. The API is now at `http://localhost/clutch/backend/api/`.
4. Run the end-to-end test (creates and deletes a throwaway account):
   ```
   C:\xampp\php\php.exe backend\tests\api_test.php
   ```

### Testing with a real phone on the same Wi-Fi
- Put this computer's address in the project's `local.properties`:
  `clutch.apiUrl=http://192.168.1.97/clutch/backend/api` (use your own IP from `ipconfig`), then rebuild the app.
- Debug builds also have a **Server address** field on the Backup screen.
- If the phone cannot connect, allow **Apache HTTP Server** through Windows Defender Firewall (Private networks).

## Deploy to a server

1. Upload the `backend/` folder (PHP 8.1+, MySQL 5.7+ / MariaDB 10.3+).
2. Import `schema.sql`.
3. Set credentials with environment variables (`CLUTCH_DB_HOST`, `CLUTCH_DB_PORT`, `CLUTCH_DB_NAME`, `CLUTCH_DB_USER`, `CLUTCH_DB_PASS`)
   **or** create `config.local.php` (git-ignored):
   ```php
   <?php return ['db_user' => 'clutch', 'db_pass' => 'a-strong-password'];
   ```
4. **Use HTTPS.** Release builds of the app refuse plain HTTP.
5. Make sure `.htaccess` files are honoured (`AllowOverride All`) so `config.php`, `lib/` and `tests/` are not downloadable.
   On Nginx, deny those paths in the server block and pass the `Authorization` header to PHP.
6. Build the app with `clutch.apiUrl=https://your-domain/clutch/backend/api` in `local.properties`.

## Security notes
- Passwords: `password_hash()` (bcrypt), re-hashed automatically when PHP's default changes.
- All SQL uses prepared statements; every synced field is type- and length-checked.
- Each account only ever reads and writes its own rows (`user_id` is part of every key).
- Not included yet: password reset by SMS, account deletion endpoint, sharing one farm between several accounts.
