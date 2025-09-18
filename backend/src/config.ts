// src/config.ts
import dotenv from 'dotenv';
dotenv.config();

function parseBool(v?: string, fallback = false) {
    if (v === undefined) return fallback;
    return v === '1' || v.toLowerCase() === 'true';
}

export const PORT = process.env.PORT ? Number(process.env.PORT) : 3000;
export const NODE_ENV = process.env.NODE_ENV || 'development';
export const MONGO_URI = process.env.MONGO_URI || 'mongodb://localhost:27017/regretnt';
export const JWT_SECRET = process.env.JWT_SECRET || 'replace-this-secret';
export const JWT_EXPIRES_IN = process.env.JWT_EXPIRES_IN || '7d';
export const ALLOWED_ORIGINS = (process.env.ALLOWED_ORIGINS || 'http://localhost:3000').split(',').map(s => s.trim());
export const ENFORCE_HTTPS = parseBool(process.env.ENFORCE_HTTPS, NODE_ENV === 'production');
export const LOG_LEVEL = process.env.LOG_LEVEL || (NODE_ENV === 'production' ? 'info' : 'debug');
export const BCRYPT_SALT_ROUNDS = process.env.BCRYPT_SALT_ROUNDS ? Number(process.env.BCRYPT_SALT_ROUNDS) : 12;

// Rate limit config
export const AUTH_RATE_LIMIT_MAX = process.env.AUTH_RATE_LIMIT_MAX ? Number(process.env.AUTH_RATE_LIMIT_MAX) : 20;
export const AUTH_RATE_LIMIT_WINDOW_MS = process.env.AUTH_RATE_LIMIT_WINDOW_MS ? Number(process.env.AUTH_RATE_LIMIT_WINDOW_MS) : 15 * 60 * 1000;

export const GOAL_COMPLETE_RATE_LIMIT_MAX = process.env.GOAL_COMPLETE_RATE_LIMIT_MAX ? Number(process.env.GOAL_COMPLETE_RATE_LIMIT_MAX) : 30;
export const GOAL_COMPLETE_RATE_LIMIT_WINDOW_MS = process.env.GOAL_COMPLETE_RATE_LIMIT_WINDOW_MS ? Number(process.env.GOAL_COMPLETE_RATE_LIMIT_WINDOW_MS) : 60 * 1000;
