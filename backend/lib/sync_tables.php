<?php
/**
 * The three synced tables, described once. Keys are the phone's field names (camelCase,
 * same as the Room entities); values are [db column, type, max length or null].
 * Types: s = string, i = int, f = float, fn = nullable float, b = bool.
 */

declare(strict_types=1);

const SYNC_TABLES = [
    'flocks' => [
        'table' => 'flocks',
        'fields' => [
            'id' => ['id', 's', 64],
            'name' => ['name', 's', 80],
            'breed' => ['breed', 's', 80],
            'hensHoused' => ['hens_housed', 'i', null],
            'placedDate' => ['placed_date', 's', 10],
            'ageWeeksAtPlacement' => ['age_weeks_at_placement', 'i', null],
            'archived' => ['archived', 'b', null],
            'updatedAt' => ['updated_at', 'i', null],
            'deleted' => ['deleted', 'b', null],
        ],
    ],
    'dailyRecords' => [
        'table' => 'daily_records',
        'fields' => [
            'id' => ['id', 's', 100],
            'flockId' => ['flock_id', 's', 64],
            'date' => ['date', 's', 10],
            'liveHens' => ['live_hens', 'i', null],
            'eggs' => ['eggs', 'i', null],
            'cracked' => ['cracked', 'i', null],
            'dirty' => ['dirty', 'i', null],
            'softShell' => ['soft_shell', 'i', null],
            'deaths' => ['deaths', 'i', null],
            'culls' => ['culls', 'i', null],
            'feedKg' => ['feed_kg', 'f', null],
            'waterL' => ['water_l', 'f', null],
            'avgEggWeightG' => ['avg_egg_weight_g', 'fn', null],
            'maxTempC' => ['max_temp_c', 'fn', null],
            'bodyWeightG' => ['body_weight_g', 'fn', null],
            'uniformityPct' => ['uniformity_pct', 'fn', null],
            'notes' => ['notes', 's', 4000],
            'enteredBy' => ['entered_by', 's', 80],
            'enteredByName' => ['entered_by_name', 's', 80],
            'updatedAt' => ['updated_at', 'i', null],
            'deleted' => ['deleted', 'b', null],
        ],
    ],
    'weeklyNotes' => [
        'table' => 'weekly_notes',
        'fields' => [
            'id' => ['id', 's', 100],
            'flockId' => ['flock_id', 's', 64],
            'ageWeek' => ['age_week', 'i', null],
            'notes' => ['notes', 's', 4000],
            'actions' => ['actions', 's', 4000],
            'updatedAt' => ['updated_at', 'i', null],
            'deleted' => ['deleted', 'b', null],
        ],
    ],
];

/** Phone row (camelCase JSON) → validated db values keyed by column. */
function row_to_db(string $key, array $row): array
{
    $out = [];
    foreach (SYNC_TABLES[$key]['fields'] as $field => [$column, $type, $max]) {
        $value = $row[$field] ?? null;
        switch ($type) {
            case 's':
                if (!is_string($value) && !is_numeric($value)) {
                    if ($field === 'notes' || $field === 'actions' || $field === 'enteredBy' || $field === 'enteredByName') {
                        $value = '';
                    } else {
                        throw new ApiError(422, 'invalid_row', "$key.$field is missing.");
                    }
                }
                $value = (string)$value;
                if ($max !== null && mb_strlen($value) > $max) {
                    throw new ApiError(422, 'invalid_row', "$key.$field is too long.");
                }
                break;
            case 'i':
                if (!is_numeric($value)) {
                    throw new ApiError(422, 'invalid_row', "$key.$field must be a number.");
                }
                $value = (int)$value;
                break;
            case 'f':
                if (!is_numeric($value)) {
                    throw new ApiError(422, 'invalid_row', "$key.$field must be a number.");
                }
                $value = (float)$value;
                break;
            case 'fn':
                $value = is_numeric($value) ? (float)$value : null;
                break;
            case 'b':
                $value = filter_var($value, FILTER_VALIDATE_BOOLEAN) ? 1 : 0;
                break;
        }
        $out[$column] = $value;
    }
    if ($out['id'] === '') {
        throw new ApiError(422, 'invalid_row', "$key.id is missing.");
    }
    return $out;
}

/** Db row → phone JSON (camelCase, proper types). */
function db_to_row(string $key, array $dbRow): array
{
    $out = [];
    foreach (SYNC_TABLES[$key]['fields'] as $field => [$column, $type, $max]) {
        $value = $dbRow[$column];
        $out[$field] = match ($type) {
            'i' => (int)$value,
            'f' => (float)$value,
            'fn' => $value === null ? null : (float)$value,
            'b' => (bool)(int)$value,
            default => (string)$value,
        };
    }
    return $out;
}
