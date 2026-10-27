<?php
/**
 * POST /api/register.php
 * Body: { "phone": "0241234567", "name": "Kofi Mensah", "password": "secret1", "device": "Tecno Spark 10" }
 * 201 → { "token": "...", "user": { "id", "phone", "name" } }
 */

declare(strict_types=1);
require __DIR__ . '/../lib/bootstrap.php';

require_method('POST');
$in = json_input();

$phone = normalize_phone((string)($in['phone'] ?? ''));
$name = trim((string)($in['name'] ?? ''));
$password = (string)($in['password'] ?? '');

if ($name === '' || mb_strlen($name) > 80) {
    throw new ApiError(422, 'invalid_name', 'Enter your name (up to 80 letters).');
}
if (strlen($password) < 6) {
    throw new ApiError(422, 'weak_password', 'Use a password of at least 6 characters.');
}

$exists = db()->prepare('SELECT id FROM users WHERE phone = ?');
$exists->execute([$phone]);
if ($exists->fetch()) {
    throw new ApiError(409, 'phone_taken', 'This phone number already has an account. Sign in instead.');
}

db()->prepare('INSERT INTO users (phone, name, password_hash) VALUES (?, ?, ?)')
    ->execute([$phone, $name, password_hash($password, PASSWORD_DEFAULT)]);
$userId = (int) db()->lastInsertId();

$token = issue_token($userId, (string)($in['device'] ?? ''));
respond(['token' => $token, 'user' => ['id' => $userId, 'phone' => $phone, 'name' => $name]], 201);
