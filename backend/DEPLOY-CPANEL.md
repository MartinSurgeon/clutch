# Put the Clutch backup server on Namecheap (cPanel)

About 20 minutes. You need: your Namecheap cPanel login, a domain on that hosting, and the file
**`clutch_backend.zip`** (in this folder's parent project, or the copy on your Desktop).

The result: `https://api.YOURDOMAIN.com` — the address the Clutch app backs up to.

> Replace **YOURDOMAIN.com** with your real domain everywhere below.
> `CPANELUSER` means your cPanel username (shown top-right in cPanel, and in front of database names).

---

## 1. Set PHP to 8.2

1. cPanel → **Software** → **Select PHP Version**.
2. **Current PHP version** → choose **8.2** (8.1 or newer works) → **Set as current**.
3. Open the **Extensions** tab and make sure these are ticked: **pdo_mysql**, **mysqlnd**, **mbstring**, **json**.
   (They save automatically.)

## 2. Create the database

1. cPanel → **Databases** → **MySQL® Database Wizard**.
2. Database name: `clutch` → **Next**. cPanel shows the full name, e.g. `CPANELUSER_clutch` — **write it down**.
3. Username: `clutchapi`. Click **Password Generator**, copy the password somewhere safe, tick that you copied it.
   Full name will be `CPANELUSER_clutchapi` — **write it down** → **Create User**.
4. Tick **ALL PRIVILEGES** → **Next Step**.

## 3. Create the tables

1. cPanel → **Databases** → **phpMyAdmin**.
2. On the left, click **CPANELUSER_clutch**.
3. Top menu → **Import** → **Choose File** → pick **`clutch_schema.sql`** from your Desktop (the same file is also
   inside the zip as `schema.sql`).
4. Click **Import** (bottom). You should see six tables: `auth_tokens`, `daily_records`, `flocks`,
   `login_attempts`, `users`, `weekly_notes`.

## 4. Create the subdomain (keeps private files off the web)

1. cPanel → **Domains** → **Domains** (or **Subdomains** on older cPanel) → **Create A New Domain**.
2. Domain: `api.YOURDOMAIN.com`.
3. **Untick** "Share document root".
4. Document Root: `clutch_backend/api`
5. **Submit**.

Only the `api` folder is now on the web. `config.local.php`, `lib/` and `schema.sql` sit one level above it,
where nobody can download them.

> If Namecheap does not let you choose a folder outside `public_html`, use `public_html/clutch_backend/api`
> as the Document Root and extract the zip inside `public_html` in step 5. The included `.htaccess` files
> then block the private files instead.

## 5. Upload the files

1. cPanel → **Files** → **File Manager**.
2. Go to your **home folder** (the folder that contains `public_html`, not inside it).
3. **Upload** → choose `clutch_backend.zip` → wait for 100% → go back.
4. Right-click `clutch_backend.zip` → **Extract** → extract to the home folder.
   You should now have `clutch_backend/` with `api/`, `lib/`, `config.php`, `schema.sql` inside.
5. Delete `clutch_backend.zip` (no longer needed).

## 6. Add the database password

1. In File Manager open `clutch_backend/`.
2. Right-click `config.local.php.example` → **Copy** → name it `config.local.php`.
3. Right-click `config.local.php` → **Edit**, and fill in the names from step 2:
   ```php
   return [
       'db_host' => 'localhost',
       'db_port' => 3306,
       'db_name' => 'CPANELUSER_clutch',
       'db_user' => 'CPANELUSER_clutchapi',
       'db_pass' => 'the password from step 2',
   ];
   ```
4. **Save Changes**.

## 7. Turn on HTTPS (free)

1. cPanel → **Security** → **SSL/TLS Status** (or the **Namecheap SSL** tool).
2. Tick `api.YOURDOMAIN.com` → **Run AutoSSL** / install the free certificate.
3. New subdomains can take up to 30 minutes for DNS + certificate. If it fails, wait and try again.

## 8. Check it works

Open in your browser: **https://api.YOURDOMAIN.com/health.php**

You want to see:
```json
{"ok":true,"service":"clutch-api","checks":{"php8_1_or_newer":true,"pdo_mysql":true,"database":true,"tables":true,"https":true}}
```

| What you see | Fix |
|---|---|
| `"database": false` | Wrong name, user or password in `config.local.php`, or the user was not given ALL PRIVILEGES (step 2.4). |
| `"tables": false` | Import `schema.sql` again into the right database (step 3). |
| `"php8_1_or_newer": false` or a blank **500** page | Set PHP 8.2 for the account (step 1). Check `clutch_backend/api/error_log` in File Manager. |
| `"https": false` | Finish step 7. |
| Browser cannot find the site | DNS for the new subdomain is still spreading. Wait 15–30 minutes. |

### Full test from your computer (optional, recommended)
In Command Prompt, in the project folder:
```
C:\xampp\php\php.exe backend\tests\api_test.php https://api.YOURDOMAIN.com
```
It signs up a throwaway account, syncs, restores, and deletes it again. All lines should say `ok`.

## 9. Point the app at the server

Send the address `https://api.YOURDOMAIN.com` to the developer (or set it yourself):
- `gradle.properties` → `clutchApiUrl=https://api.YOURDOMAIN.com` (committed, so Google AI Studio builds use it too)
- Remove `clutch.apiUrl` from `local.properties` on the development computer (that line points at the test PC).
- Rebuild the app.

---

## If something goes wrong later

- **Sync fails with "Not Acceptable" or 403 on save:** Namecheap's ModSecurity firewall sometimes blocks
  form-like content. Ask Namecheap support to whitelist `api.YOURDOMAIN.com` for ModSecurity.
- **Opening a folder in the browser shows a list of files:** cPanel → **Advanced** → **Indexes** → open the
  domain's folder → **No Indexing** → **Save**. Also delete any `clutch_backend.zip` left on the server.
- **Everyone is suddenly signed out:** check that `config.local.php` was not changed or deleted.
- **Backups:** cPanel → **Backup** → download a **MySQL database backup** of `CPANELUSER_clutch` regularly
  (or use Namecheap's automatic backups).
- **Never** put `config.local.php` inside `public_html` without the `.htaccess` files next to it.
