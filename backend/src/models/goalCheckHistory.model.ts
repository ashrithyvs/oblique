// src/models/goalCheckHistory.model.ts
import mongoose, { Document, Schema, Types } from 'mongoose';

export interface IGoalCheckHistory extends Document {
  goal: Types.ObjectId;
  user: Types.ObjectId;
  checkedAt: Date;
  result: 'completed' | 'no-change' | 'error';
  details?: any;
  evidence?: any;
  errorMessage?: string | null;
}

const GoalCheckHistorySchema = new Schema<IGoalCheckHistory>({
  goal: { type: Schema.Types.ObjectId, ref: 'Goal', required: true, index: true },
  user: { type: Schema.Types.ObjectId, ref: 'User', required: true },
  checkedAt: { type: Date, required: true, default: () => new Date() },
  result: { type: String, enum: ['completed', 'no-change', 'error'], required: true },
  details: { type: Schema.Types.Mixed },
  evidence: { type: Schema.Types.Mixed },
  errorMessage: { type: String, default: null }
}, { timestamps: true });

GoalCheckHistorySchema.index({ goal: 1, checkedAt: -1 });

const GoalCheckHistory = mongoose.model<IGoalCheckHistory>('GoalCheckHistory', GoalCheckHistorySchema);
export default GoalCheckHistory;
