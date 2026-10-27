<?php
/**
 * POST /api/login.php
 * Body: { "phone": "0241234567", "password": "secret1", "device": "Tecno Spark 10" }
 * 200 → { "token": "...", "user": { "id", "phone", "name" } }
 * Too many failed tries for one number → 429 for a short while.
 */

declare(strict_types=1);
require __DIR__ . '/../lib/bootstrap.php';

require_method('POST');
$in = json_input();

$phone = normalize_phone((string)($in['phone'] ?? ''));
$password = (string)($in['password'] ?? '');
$c = config();

$recent = db()->prepare(
    'SELECT COUNT(*) FROM login_attempts WHERE phone = ? AND attempted_at > (NOW() - INTERVAL ? MINUTE)'
);
$recent->execute([$phone, $c['login_window_minutes']]);
if ((int)$recent->fetchColumn() >= $c['max_login_attempts']) {
    throw new ApiError(429, 'too_many_attempts', "Too many wrong tries. Wait {$c['login_window_minutes']} minutes and try again.");
}

$stmt = db()->prepare('SELECT id, phone, name, password_hash FROM users WHERE phone = ?');
$stmt->execute([$phone]);
$user = $stmt->fetch();

if (!$user || !password_verify($password, $user['password_hash'])) {
    db()->prepare('INSERT INTO login_attempts (phone, ip) VALUES (?, ?)')->execute([$phone, client_ip()]);
    throw new ApiError(401, 'wrong_credentials', 'Phone number or password is wrong.');
}

if (password_needs_rehash($user['password_hash'], PASSWORD_DEFAULT)) {
    db()->prepare('UPDATE users SET password_hash = ? WHERE id = ?')
        ->execute([password_hash($password, PASSWORD_DEFAULT), $user['id']]);
}
db()->prepare('DELETE FROM login_attempts WHERE phone = ?')->execute([$phone]);

$token = issue_token((int)$user['id'], (string)($in['device'] ?? ''));
respond(['token' => $token, 'user' => public_user($user)]);
