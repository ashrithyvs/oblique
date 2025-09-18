export interface GoalDTO {
    id: string;
    platform: string;
    unit: string;
    targetValue: number;
    progress: number;
    createdAt: string;
    deadline?: string | null;
}
