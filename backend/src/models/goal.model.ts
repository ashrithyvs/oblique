// src/models/goal.model.ts
import mongoose, { Document, Schema, Types } from 'mongoose';

export interface IGoal extends Document {
    user: Types.ObjectId;
    title: string;
    platform: string;
    platformUsername?: string | null;
    platformIcon?: Buffer | null;    // optional platform icon blob (if ever uploaded)
    unit?: string;                   // e.g. "minutes", "lessons"
    baselineValue: number;           // baseline snapshot (used for validation)
    targetValue: number;             // required target for completion
    progress: number;                // optional local progress snapshot
    status: 'active' | 'completed' | 'paused';
    deadline?: number | null;
    checkIntervalMs: number;         // how often to poll/check for completion
    lastCheckedAt?: Date | null;
    completedAt?: Date | null;
    completedByDevice?: boolean;     // whether device-side checker completed it
    evidence?: Schema.Types.Mixed;   // arbitrary JSON metadata/evidence
    createdAt: Date;
    updatedAt: Date;
}

const GoalSchema = new Schema<IGoal>(
    {
        user: { type: Schema.Types.ObjectId, ref: 'User', required: true },
        title: { type: String, required: true },
        platform: { type: String, required: true }, // e.g. "leetcode"
        platformUsername: { type: String, default: null },
        platformIcon: { type: Buffer, default: null },

        unit: { type: String, default: '' },
        baselineValue: { type: Number, default: 0 },
        targetValue: { type: Number, required: true },
        progress: { type: Number, default: 0 },
        status: { type: String, enum: ['active', 'completed', 'paused'], default: 'active' },
        checkIntervalMs: { type: Number, default: 3600000 }, // default 1 hour
        deadline: { type: Number, default: null },
        lastCheckedAt: { type: Date, default: null },
        completedAt: { type: Date, default: null },
        completedByDevice: { type: Boolean, default: false },

        evidence: { type: Schema.Types.Mixed, default: null }
    },
    {
        timestamps: true
    }
);

// Virtual for a quick computed "completed" flag
GoalSchema.virtual('completed').get(function (this: IGoal) {
    return (this.progress || 0) >= (this.targetValue || 0);
});

// Index to accelerate per-user queries
GoalSchema.index({ user: 1, createdAt: -1 });

const Goal = mongoose.model<IGoal>('Goal', GoalSchema);
export default Goal;
