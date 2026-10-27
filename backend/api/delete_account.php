<?php
/**
 * POST /api/delete_account.php  (Authorization: Bearer <token>)
 * Body: { "password": "secret1" }
 * Permanently deletes the account and every backed-up record (required by Google Play
 * for apps that let people create accounts). Records on the phone are not touched.
 * 200 → { "ok": true }   403 → wrong password
 */

declare(strict_types=1);
require __DIR__ . '/../lib/bootstrap.php';

require_method('POST');
$user = require_user();
$in = json_input();

$stmt = db()->prepare('SELECT password_hash FROM users WHERE id = ?');
$stmt->execute([$user['id']]);
$hash = (string) $stmt->fetchColumn();

if (!password_verify((string)($in['password'] ?? ''), $hash)) {
    throw new ApiError(403, 'wrong_password', 'That password is not correct.');
}

// Tokens and all farm rows go with the user (ON DELETE CASCADE)
db()->prepare('DELETE FROM users WHERE id = ?')->execute([$user['id']]);
db()->prepare('DELETE FROM login_attempts WHERE phone = ?')->execute([$user['phone']]);

respond(['ok' => true]);
