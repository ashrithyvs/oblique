// src/models/blockedApp.model.ts
import mongoose, { Document, Schema, Types } from 'mongoose';

export interface IBlockedApp extends Document {
    user: Types.ObjectId;
    packageName: string;
    reason?: string;
    createdAt: Date;
    updatedAt: Date;
}

const BlockedAppSchema = new Schema<IBlockedApp>({
    user: { type: Schema.Types.ObjectId, ref: 'User', required: true },
    packageName: { type: String, required: true, index: true },
    reason: { type: String }
}, { timestamps: true });

BlockedAppSchema.index({ user: 1, packageName: 1 }, { unique: true });

const BlockedApp = mongoose.model<IBlockedApp>('BlockedApp', BlockedAppSchema);
export default BlockedApp;
