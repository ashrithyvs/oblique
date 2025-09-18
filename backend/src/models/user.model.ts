// src/models/user.model.ts
import mongoose, { Document, Schema, Types } from 'mongoose';

export interface IOAuthProvider {
    provider: string; // 'google', 'facebook', etc
    providerId: string;
    meta?: any;
}

export interface IUser extends Document {
    email?: string | null;
    name?: string | null;
    onboardingCompleted?: boolean;
    blockedApps: string[]; // list of package names
    hashedPin?: string | null;
    hashedPassword?: string | null;
    providers?: IOAuthProvider[];
    icon?: Buffer | null;
    createdAt: Date;
    updatedAt: Date;
}

const OAuthProviderSchema = new Schema({
    provider: { type: String, required: true },
    providerId: { type: String, required: true },
    meta: { type: Schema.Types.Mixed }
}, { _id: false });

const UserSchema = new Schema<IUser>({
    email: { type: String, sparse: true },
    name: { type: String },
    onboardingCompleted: { type: Boolean, default: false },
    blockedApps: { type: [String], default: [] }, // now an array of simple strings (package names)
    hashedPin: { type: String, default: null, select: false },
    hashedPassword: { type: String, default: null, select: false }, // new: hashed password for login
    providers: { type: [OAuthProviderSchema], default: [] }, // external auth providers (google etc)
    icon: { type: Buffer, select: false }
}, { timestamps: true });

// Indexes
UserSchema.index({ email: 1 });
UserSchema.index({ onboardingCompleted: 1 });

const User = mongoose.model<IUser>('User', UserSchema);
export default User;
