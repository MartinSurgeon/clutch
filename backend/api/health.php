<?php
/**
 * GET /api/health.php — open in a browser after deploying.
 * 200 { "ok": true, ... } when PHP, the database and the tables are all ready.
 * 503 with the failing check otherwise. Never shows passwords or versions.
 */

declare(strict_types=1);
require __DIR__ . '/../lib/bootstrap.php';

$checks = [
    'php8_1_or_newer' => PHP_VERSION_ID >= 80100,
    'pdo_mysql' => extension_loaded('pdo_mysql'),
    'database' => false,
    'tables' => false,
    'https' => (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off')
        || (($_SERVER['HTTP_X_FORWARDED_PROTO'] ?? '') === 'https'),
];
$hint = null;
$missing = [];

try {
    $tables = db()->query('SHOW TABLES')->fetchAll(PDO::FETCH_COLUMN);
    $checks['database'] = true;
    $missing = array_values(array_diff(
        ['users', 'auth_tokens', 'login_attempts', 'flocks', 'daily_records', 'weekly_notes'],
        $tables
    ));
    $checks['tables'] = $missing === [];
    if (!$checks['tables']) {
        $hint = 'Import schema.sql into the database with phpMyAdmin.';
    }
} catch (Throwable $e) {
    $hint = 'Cannot open the database. Check the name, user and password in config.local.php, and that the user has ALL PRIVILEGES on the database.';
}

$ok = $checks['php8_1_or_newer'] && $checks['pdo_mysql'] && $checks['database'] && $checks['tables'];
if ($ok && !$checks['https']) {
    $hint = 'Working, but not over HTTPS. Install the free SSL certificate — release builds of the app need HTTPS.';
}

$body = ['ok' => $ok, 'service' => 'clutch-api', 'checks' => $checks];
if ($missing) {
    $body['missingTables'] = $missing;
}
if ($hint) {
    $body['hint'] = $hint;
}
respond($body, $ok ? 200 : 503);
