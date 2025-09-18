// src/db.ts
import mongoose from 'mongoose';
import logger from '../utils/logger';
import { MONGO_URI } from '../config';

export async function connectToMongo(): Promise<void> {
  mongoose.set('strictQuery', false);
  try {
    logger.info(`Connecting to MongoDB: ${MONGO_URI}`);
    await mongoose.connect(MONGO_URI, {
      // recommended options
      // useNewUrlParser / useUnifiedTopology are defaults in modern mongoose
      autoIndex: true, // set to false in prod for large DBs
    } as any);
    logger.info('MongoDB connected');
    mongoose.connection.on('disconnected', () => logger.warn('MongoDB disconnected'));
    mongoose.connection.on('reconnected', () => logger.info('MongoDB reconnected'));
    mongoose.connection.on('error', (err) => logger.error('MongoDB error', err));
  } catch (err) {
    logger.error('Failed to connect to MongoDB', err as any);
    throw err;
  }
}

export async function closeMongo(): Promise<void> {
  try {
    await mongoose.disconnect();
    logger.info('MongoDB disconnected (closed)');
  } catch (err) {
    logger.error('Error while disconnecting MongoDB', err as any);
  }
}
