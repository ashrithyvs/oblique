// src/utils/hash.ts
import bcrypt from 'bcrypt';
import { BCRYPT_SALT_ROUNDS } from '../config';

// default salt rounds if not provided in config
const SALT_ROUNDS = (typeof BCRYPT_SALT_ROUNDS !== 'undefined') ? Number(BCRYPT_SALT_ROUNDS) : 10;

/**
 * Hash a password (for normal auth).
 */
export async function hashPassword(password: string): Promise<string> {
  const salt = await bcrypt.genSalt(SALT_ROUNDS);
  return bcrypt.hash(password, salt);
}

/**
 * Verify password.
 */
export async function verifyPassword(password: string, hashed: string): Promise<boolean> {
  return bcrypt.compare(password, hashed);
}

/**
 * Hash a PIN (short numeric code). We use bcrypt as well (safer than plain storage),
 * but you may want to use a different policy if you treat PINs specially.
 */
export async function hashPin(pin: string): Promise<string> {
  const salt = await bcrypt.genSalt(SALT_ROUNDS);
  return bcrypt.hash(pin, salt);
}

/**
 * Verify PIN.
 */
export async function verifyPin(pin: string, hashed: string): Promise<boolean> {
  return bcrypt.compare(pin, hashed);
}
