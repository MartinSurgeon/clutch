<?php
/**
 * GET /api/me.php  (Authorization: Bearer <token>)
 * 200 → { "user": {...}, "counts": { "flocks", "dailyRecords", "weeklyNotes" } }
 */

declare(strict_types=1);
require __DIR__ . '/../lib/bootstrap.php';

require_method('GET');
$user = require_user();

$counts = [];
foreach (['flocks' => 'flocks', 'dailyRecords' => 'daily_records', 'weeklyNotes' => 'weekly_notes'] as $key => $table) {
    $stmt = db()->prepare("SELECT COUNT(*) FROM $table WHERE user_id = ? AND deleted = 0");
    $stmt->execute([$user['id']]);
    $counts[$key] = (int)$stmt->fetchColumn();
}

respond(['user' => public_user($user), 'counts' => $counts]);
