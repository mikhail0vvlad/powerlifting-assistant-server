-- Add schedule configuration to programs (encoded as a simple string:
--   "weekdays:1,3,5"  — ISO weekday numbers (1=Mon..7=Sun)
--   "dates:2026-05-01,2026-05-04,..."  — explicit list of training dates
-- NULL means legacy/auto Mon-Wed-Fri).
ALTER TABLE training_programs
    ADD COLUMN IF NOT EXISTS schedule_json TEXT;

-- Track that a workout was created as the result of rescheduling another one.
-- Self-FK with ON DELETE SET NULL so the chain breaks gracefully if the source
-- workout gets deleted (e.g. when the whole program is regenerated).
ALTER TABLE program_workouts
    ADD COLUMN IF NOT EXISTS original_workout_id UUID
    REFERENCES program_workouts(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_program_workouts_original
    ON program_workouts(original_workout_id);

CREATE INDEX IF NOT EXISTS idx_program_workouts_status_date
    ON program_workouts(program_id, status, workout_date);
