<?php
/**
 * POST /api/sync.php  (Authorization: Bearer <token>)
 *
 * One round trip that pushes the phone's changes and pulls everyone else's.
 *
 * Body:
 *   {
 *     "since": 0,                    // server cursor from the last sync (0 = first sync / restore)
 *     "changes": {
 *       "flocks": [ {...} ], "dailyRecords": [ {...} ], "weeklyNotes": [ {...} ]
 *     }
 *   }
 *
 * Rules:
 *   - Last write wins by updatedAt (the time the farmer made the change). Deletes are rows with deleted=true.
 *   - A pushed row older than the server copy is not stored; the server copy is sent back instead.
 *
 * 200 →
 *   {
 *     "serverTime": 1730000000000,   // store as the next "since"
 *     "more": false,                 // true = call again with the new serverTime to get the rest
 *     "accepted": { "flocks": ["id", ...], "dailyRecords": [...], "weeklyNotes": [...] },
 *     "changes":  { "flocks": [ {...} ], "dailyRecords": [...], "weeklyNotes": [...] }
 *   }
 */

declare(strict_types=1);
require __DIR__ . '/../lib/bootstrap.php';
require __DIR__ . '/../lib/sync_tables.php';

require_method('POST');
$user = require_user();
$userId = (int)$user['id'];
$in = json_input();

$since = isset($in['since']) && is_numeric($in['since']) ? max(0, (int)$in['since']) : 0;
$changes = is_array($in['changes'] ?? null) ? $in['changes'] : [];
$limit = (int)config()['sync_batch_limit'];

$accepted = [];
$rejectedIds = [];
$pdo = db();
$pdo->beginTransaction();
try {
    foreach (SYNC_TABLES as $key => $spec) {
        $accepted[$key] = [];
        $rejectedIds[$key] = [];
        $rows = $changes[$key] ?? [];
        if (!is_array($rows)) {
            throw new ApiError(422, 'invalid_changes', "changes.$key must be a list.");
        }
        if (count($rows) > $limit) {
            throw new ApiError(413, 'too_many_rows', "Send at most $limit $key at a time.");
        }

        $table = $spec['table'];
        $columns = array_map(fn($f) => $f[0], array_values($spec['fields']));
        $insertCols = array_merge(['user_id'], $columns, ['server_updated_at']);
        $placeholders = implode(', ', array_fill(0, count($insertCols), '?'));
        $updates = implode(', ', array_map(fn($c) => "$c = VALUES($c)", array_merge($columns, ['server_updated_at'])));
        $upsert = $pdo->prepare(
            "INSERT INTO $table (" . implode(', ', $insertCols) . ") VALUES ($placeholders) ON DUPLICATE KEY UPDATE $updates"
        );
        $current = $pdo->prepare("SELECT updated_at FROM $table WHERE user_id = ? AND id = ? FOR UPDATE");

        foreach ($rows as $row) {
            if (!is_array($row)) {
                throw new ApiError(422, 'invalid_row', "Each $key item must be an object.");
            }
            $values = row_to_db($key, $row);
            $current->execute([$userId, $values['id']]);
            $existing = $current->fetchColumn();

            if ($existing !== false && (int)$existing > $values['updated_at']) {
                $rejectedIds[$key][] = $values['id'];
                continue;
            }
            $upsert->execute(array_merge([$userId], array_values($values), [now_ms()]));
            $accepted[$key][] = $values['id'];
        }
    }
    $pdo->commit();
} catch (Throwable $e) {
    $pdo->rollBack();
    throw $e;
}

// Pull: everything stored after the phone's cursor, plus the server copy of any rejected rows
$serverTime = now_ms();
$more = false;
$out = [];
foreach (SYNC_TABLES as $key => $spec) {
    $table = $spec['table'];
    $stmt = $pdo->prepare(
        "SELECT * FROM $table WHERE user_id = ? AND server_updated_at > ? ORDER BY server_updated_at ASC LIMIT " . ($limit + 1)
    );
    $stmt->execute([$userId, $since]);
    $rows = $stmt->fetchAll();
    if (count($rows) > $limit) {
        $rows = array_slice($rows, 0, $limit);
        $more = true;
        // Step back 1 ms so rows sharing the cut-off time are sent again next page, never skipped.
        // Re-sent rows are harmless: applying the same row twice changes nothing.
        $cutoff = (int)end($rows)['server_updated_at'];
        $serverTime = min($serverTime, $cutoff - 1 > $since ? $cutoff - 1 : $cutoff);
    }
    $out[$key] = array_map(fn($r) => db_to_row($key, $r), $rows);

    if ($rejectedIds[$key]) {
        $in_ = implode(', ', array_fill(0, count($rejectedIds[$key]), '?'));
        $stmt = $pdo->prepare("SELECT * FROM $table WHERE user_id = ? AND id IN ($in_)");
        $stmt->execute(array_merge([$userId], $rejectedIds[$key]));
        $seen = array_column($out[$key], 'id');
        foreach ($stmt->fetchAll() as $r) {
            if (!in_array($r['id'], $seen, true)) {
                $out[$key][] = db_to_row($key, $r);
            }
        }
    }
}

respond([
    'serverTime' => $serverTime,
    'more' => $more,
    'accepted' => $accepted,
    'changes' => $out,
]);
