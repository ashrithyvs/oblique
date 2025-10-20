import mongoose, { Document, Schema, Types } from 'mongoose';

export interface IOAuthProvider {
    provider: string; // 'google', 'facebook', etc
    providerId: string;
    meta?: any;
}

export interface IUser extends Document {
    email?: string | null;
    name?: string | null;
    displayName?: string | null;                // ✅ user’s display name
    onboardingCompleted?: boolean;
    blockedApps: string[]; 
    hashedPin?: string | null;
    hashedPassword?: string | null;
    providers?: IOAuthProvider[];
    icon?: Buffer | null;
    platformUsernames?: Record<string, string>;   // 👈 plain object
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
    displayName: { type: String, default: null },                  // ✅ new
    onboardingCompleted: { type: Boolean, default: false },
    blockedApps: { type: [String], default: [] },
    hashedPin: { type: String, default: null, select: false },
    hashedPassword: { type: String, default: null, select: false },
    providers: { type: [OAuthProviderSchema], default: [] },
    icon: { type: Buffer, select: false },
    platformUsernames: { type: Object, default: {} },
}, { timestamps: true });

UserSchema.index({ email: 1 });
UserSchema.index({ onboardingCompleted: 1 });

const User = mongoose.model<IUser>('User', UserSchema);
export default User;