// src/models/goal.model.ts
import mongoose, { Document, Schema, Types } from 'mongoose';

export interface IGoal extends Document {
    user: Types.ObjectId;
    title?: string;
    platform: string;
    platformUsername: string;
    targetValue: number;
    baselineValue?: number;
    status: 'active' | 'completed';
    deadline?: Date;
    checkIntervalMs?: number;
    evidence?: any;
    completedAt?: Date | null;
    createdAt: Date;
    updatedAt: Date;
}

const GoalSchema = new Schema<IGoal>({
    user: { type: Schema.Types.ObjectId, ref: 'User', required: true, index: true },
    title: { type: String },
    platform: { type: String, required: true, default: 'leetcode' },
    platformUsername: { type: String, required: true },
    targetValue: { type: Number, required: true },
    baselineValue: { type: Number, default: 0 },
    status: { type: String, enum: ['active', 'completed'], default: 'active' },
    deadline: { type: Date },
    checkIntervalMs: { type: Number, default: 3600000 },
    evidence: { type: Schema.Types.Mixed },
    completedAt: { type: Date, default: null }
}, { timestamps: true });

const Goal = mongoose.model<IGoal>('Goal', GoalSchema);
export default Goal;
