// src/logger.ts
import winston from 'winston';
import { LOG_LEVEL, NODE_ENV } from '../config';

const enumerateErrorFormat = winston.format((info) => {
  if (info instanceof Error) {
    return Object.assign({}, info, {
      message: info.message,
      stack: info.stack
    });
  }
  return info;
});

export const logger = winston.createLogger({
  level: LOG_LEVEL,
  format: winston.format.combine(
    enumerateErrorFormat(),
    winston.format.timestamp(),
    NODE_ENV === 'production'
      ? winston.format.json()
      : winston.format.printf(info => `${info.timestamp} [${info.level}] ${info.message}${info.stack ? '\n' + info.stack : ''}`)
  ),
  transports: [
    new winston.transports.Console()
  ],
  exitOnError: false,
});

// morgan stream to winston
export const morganStream = {
  write: (message: string) => {
    logger.info(message.trim());
  }
};

export default logger;
