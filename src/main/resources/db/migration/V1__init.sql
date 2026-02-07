-- Enable UUID generation
CREATE EXTENSION IF NOT EXISTS pgcrypto;

CREATE TABLE IF NOT EXISTS users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    firebase_uid TEXT UNIQUE NOT NULL,
    email TEXT,
    display_name TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS user_profile (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    height_cm INT,
    weight_kg NUMERIC(5,2),
    bench_1rm NUMERIC(6,2),
    squat_1rm NUMERIC(6,2),
    deadlift_1rm NUMERIC(6,2),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE IF NOT EXISTS nutrition_goals (
    user_id UUID PRIMARY KEY REFERENCES users(id) ON DELETE CASCADE,
    calories_goal INT NOT NULL DEFAULT 2500,
    protein_goal_g INT NOT NULL DEFAULT 150
);

CREATE TABLE IF NOT EXISTS nutrition_entries (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    eaten_at TIMESTAMPTZ NOT NULL,
    title TEXT NOT NULL,
    calories INT NOT NULL,
    protein_g INT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_nutrition_entries_user_time ON nutrition_entries(user_id, eaten_at);

CREATE TABLE IF NOT EXISTS training_programs (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    name TEXT NOT NULL,
    template_code TEXT NOT NULL,
    start_date DATE NOT NULL,
    weeks INT NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_programs_user_active ON training_programs(user_id, is_active);

CREATE TABLE IF NOT EXISTS program_workouts (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    program_id UUID NOT NULL REFERENCES training_programs(id) ON DELETE CASCADE,
    workout_date DATE NOT NULL,
    title TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'planned'
);

CREATE INDEX IF NOT EXISTS idx_program_workouts_date ON program_workouts(program_id, workout_date);

CREATE TABLE IF NOT EXISTS program_exercises (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    program_workout_id UUID NOT NULL REFERENCES program_workouts(id) ON DELETE CASCADE,
    exercise_name TEXT NOT NULL,
    order_index INT NOT NULL,
    sets INT NOT NULL,
    reps TEXT NOT NULL,
    percent_1rm NUMERIC(5,2),
    lift_type TEXT NOT NULL DEFAULT 'other'
);

CREATE INDEX IF NOT EXISTS idx_program_exercises_workout ON program_exercises(program_workout_id);

CREATE TABLE IF NOT EXISTS workout_sessions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    program_workout_id UUID REFERENCES program_workouts(id) ON DELETE SET NULL,
    started_at TIMESTAMPTZ NOT NULL,
    finished_at TIMESTAMPTZ,
    workout_duration_sec INT,

    sleep_hours NUMERIC(3,1),
    wellbeing INT,
    fatigue INT,
    soreness INT,
    recommendation TEXT
);

CREATE INDEX IF NOT EXISTS idx_workout_sessions_user_time ON workout_sessions(user_id, started_at);

CREATE TABLE IF NOT EXISTS workout_sets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    session_id UUID NOT NULL REFERENCES workout_sessions(id) ON DELETE CASCADE,
    exercise_name TEXT NOT NULL,
    set_number INT NOT NULL,
    weight_kg NUMERIC(6,2) NOT NULL,
    reps INT NOT NULL,
    rpe NUMERIC(3,1)
);

CREATE INDEX IF NOT EXISTS idx_workout_sets_session ON workout_sets(session_id);

CREATE TABLE IF NOT EXISTS achievements (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    note TEXT NOT NULL,
    photo_url TEXT
);

CREATE INDEX IF NOT EXISTS idx_achievements_user_time ON achievements(user_id, created_at);
