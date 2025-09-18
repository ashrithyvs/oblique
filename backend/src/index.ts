// src/index.ts
import app from './app';
import { connectToMongo, closeMongo } from './database/configDB';
import { logger } from './utils/logger';
import { PORT, NODE_ENV } from './config';

async function start() {
    try {
        await connectToMongo();
        const server = app.listen(PORT, () => {
            logger.info(`Server running in ${NODE_ENV} mode on port ${PORT}`);
        });

        const shutdown = async (signal: string) => {
            try {
                logger.info(`Received ${signal}, shutting down gracefully...`);
                server.close(async (err?: any) => {
                    if (err) {
                        logger.error('Error closing HTTP server', err);
                        process.exit(1);
                    }
                    await closeMongo();
                    logger.info('Shutdown complete, exiting.');
                    process.exit(0);
                });
            } catch (err) {
                logger.error('Error during shutdown', err);
                process.exit(1);
            }
        };

        process.on('SIGINT', () => shutdown('SIGINT'));
        process.on('SIGTERM', () => shutdown('SIGTERM'));
        process.on('uncaughtException', (err) => {
            logger.error('uncaughtException', err);
            process.exit(1);
        });
        process.on('unhandledRejection', (reason) => {
            logger.error('unhandledRejection', reason as any);
        });
    } catch (err) {
        logger.error('Failed to start server', err);
        process.exit(1);
    }
}

start();
