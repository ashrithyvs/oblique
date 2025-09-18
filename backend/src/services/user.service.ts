// src/services/user.service.ts
import User, { IUser } from '../models/user.model';
import { hashPin, verifyPin, hashPassword, verifyPassword } from '../utils/hash';
import mongoose from 'mongoose';

export async function createUser(payload: { email?: string; name?: string; pin?: string; password?: string; providers?: any[] }): Promise<IUser> {
    const doc: Partial<IUser> = { email: payload.email, name: payload.name };
    if (payload.pin) {
        doc.hashedPin = await hashPin(payload.pin);
    }
    if (payload.password) {
        doc.hashedPassword = await hashPassword(payload.password);
    }
    if (payload.providers && Array.isArray(payload.providers)) {
        doc.providers = payload.providers;
    }
    const user = new User(doc);
    await user.save();
    return user;
}

export async function findUserByEmail(email: string) {
    return User.findOne({ email }).exec();
}

export async function setPinForUser(userId: string, pin: string) {
    const hashed = await hashPin(pin);
    await User.findByIdAndUpdate(userId, { hashedPin: hashed }).exec();
}

export async function verifyUserPin(userId: string, pin: string) {
    const u = await User.findById(userId).select('+hashedPin').exec();
    if (!u || !u.hashedPin) return false;
    return await verifyPin(pin, u.hashedPin);
}

export async function setPasswordForUser(userId: string, password: string) {
    const hashed = await hashPassword(password);
    await User.findByIdAndUpdate(userId, { hashedPassword: hashed }).exec();
}

export async function verifyUserPasswordByEmail(email: string, password: string) {
    const u = await User.findOne({ email }).select('+hashedPassword').exec();
    if (!u || !u.hashedPassword) return null;
    const ok = await verifyPassword(password, u.hashedPassword);
    return ok ? u : null;
}

export async function uploadUserIcon(userId: string, buffer: Buffer) {
    await User.findByIdAndUpdate(userId, { icon: buffer }).exec();
}

/**
 * Update the user's blocked apps list (array of package names).
 */
export async function updateBlockedApps(userId: string, blockedApps: string[]) {
    await User.findByIdAndUpdate(userId, { blockedApps }).exec();
}
