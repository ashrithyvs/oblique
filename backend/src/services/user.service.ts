// src/services/user.service.ts
import mongoose from 'mongoose';
import User, { IUser } from '../models/user.model';
import { hashPin, verifyPin, hashPassword, verifyPassword } from '../utils/hash';

/**
 * Create new user (keeps fields you had: hashedPin, hashedPassword, providers, icon)
 */
export async function createUser(payload: {
    email?: string | null;
    name?: string | null;
    pin?: string | null;
    password?: string | null;
    providers?: any[];
}): Promise<IUser> {
    const doc: Partial<IUser> = {
        email: payload.email ?? null,
        name: payload.name ?? null,
    };

    if (payload.pin) {
        doc.hashedPin = await hashPin(payload.pin);
    }
    if (payload.password) {
        doc.hashedPassword = await hashPassword(payload.password);
    }
    if (payload.providers) {
        doc.providers = payload.providers;
    }

    const u = await User.create(doc as any);
    return u;
}

export async function findUserByEmail(email?: string | null) {
    if (!email) return null;
    return User.findOne({ email }).exec();
}

export async function getUserById(id: string) {
    if (!mongoose.isValidObjectId(id)) return null;
    // exclude secret fields by default
    return User.findById(id).select('-hashedPassword -hashedPin').lean().exec();
}

export async function getUserDocumentById(id: string) {
    // returns full mongoose doc (including hashed fields) when server logic needs to update them
    if (!mongoose.isValidObjectId(id)) return null;
    return User.findById(id).exec();
}

export async function setUserPin(userId: string, pin: string) {
    const hashed = await hashPin(pin);
    await User.findByIdAndUpdate(userId, { hashedPin: hashed }).exec();
}

export async function verifyUserPin(userId: string, pin: string) {
    const u = await User.findById(userId).select('hashedPin').exec();
    if (!u || !u.hashedPin) return false;
    return verifyPin(pin, u.hashedPin);
}

export async function setUserPassword(userId: string, password: string) {
    const hashed = await hashPassword(password);
    await User.findByIdAndUpdate(userId, { hashedPassword: hashed }).exec();
}

/**
 * 🔑 Verify user password by email (used in login)
 */
export async function verifyUserPasswordByEmail(email: string, password: string) {
    const u = await User.findOne({ email }).select('hashedPassword _id email name').exec();
    if (!u || !u.hashedPassword) return null;
    const ok = await verifyPassword(password, u.hashedPassword);
    return ok ? u : null;
}

/**
 * Legacy naming kept for backwards compatibility
 */
export const verifyUserPassword = verifyUserPasswordByEmail;

export async function uploadUserIcon(userId: string, buffer: Buffer) {
    await User.findByIdAndUpdate(userId, { icon: buffer }).exec();
}

/* ---------------------- Blocked apps (user-embedded) ---------------------- */

/**
 * Return blocked apps as an array of strings (package names).
 */
export async function getBlockedApps(userId: string): Promise<string[]> {
    const u = await User.findById(userId).select('blockedApps').lean().exec();
    return (u && (u as any).blockedApps) || [];
}

/**
 * Add a blocked app (idempotent).
 */
export async function addBlockedApp(userId: string, packageName: string) {
    if (!packageName || typeof packageName !== 'string') return;
    await User.findByIdAndUpdate(userId, { $addToSet: { blockedApps: packageName } }).exec();
}

/**
 * Remove a blocked app by package name.
 */
export async function removeBlockedApp(userId: string, packageName: string) {
    if (!packageName || typeof packageName !== 'string') return;
    await User.findByIdAndUpdate(userId, { $pull: { blockedApps: packageName } }).exec();
}

/**
 * Replace entire blockedApps list (useful for bulk edits from client).
 */
export async function updateBlockedApps(userId: string, apps: string[]) {
    const cleaned = Array.isArray(apps) ? apps.filter(x => typeof x === 'string' && x.trim().length > 0) : [];
    await User.findByIdAndUpdate(userId, { blockedApps: cleaned }).exec();
}
