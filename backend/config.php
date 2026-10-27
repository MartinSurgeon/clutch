<?php
/**
 * Clutch API configuration.
 * Values come from environment variables in production; the defaults suit a local XAMPP install.
 * For a server, either set the env vars or create config.local.php (git-ignored) that returns overrides.
 */

$config = [
    'db_host' => getenv('CLUTCH_DB_HOST') ?: '127.0.0.1',
    'db_port' => (int)(getenv('CLUTCH_DB_PORT') ?: 3306),
    'db_name' => getenv('CLUTCH_DB_NAME') ?: 'clutch',
    'db_user' => getenv('CLUTCH_DB_USER') ?: 'root',
    'db_pass' => getenv('CLUTCH_DB_PASS') ?: '',
    // Failed sign-ins allowed per phone number within the window below
    'max_login_attempts' => 5,
    'login_window_minutes' => 15,
    // Largest number of rows accepted or returned per table in one sync call
    'sync_batch_limit' => 2000,
];

$localFile = __DIR__ . '/config.local.php';
if (is_file($localFile)) {
    $config = array_merge($config, require $localFile);
}

return $config;
