// src/app.ts
import express from 'express';
import helmet from 'helmet';
import cors from 'cors';
import morgan from 'morgan';
import rateLimit from 'express-rate-limit';
import compression from 'compression';
import routes from './routes';
import { errorHandler } from './middleware/errorHandler';
import { AUTH_RATE_LIMIT_MAX, AUTH_RATE_LIMIT_WINDOW_MS, GOAL_COMPLETE_RATE_LIMIT_MAX, GOAL_COMPLETE_RATE_LIMIT_WINDOW_MS, ALLOWED_ORIGINS, ENFORCE_HTTPS, PORT } from './config';
import { logger, morganStream } from './utils/logger';

const app = express();

// Security
app.use(helmet());
app.use(compression());
app.use(express.json({ limit: '2mb' }));

// Logging
app.use(morgan('combined', { stream: morganStream }));

// CORS - restrict origins to ALLOWED_ORIGINS
const corsOptions: cors.CorsOptions = {
    origin: (origin, callback) => {
        // allow requests with no origin (like mobile apps or curl)
        if (!origin) return callback(null, true);
        if (ALLOWED_ORIGINS.indexOf(origin) !== -1) {
            return callback(null, true);
        } else {
            return callback(new Error('Not allowed by CORS'), false);
        }
    },
    methods: ['GET', 'POST', 'PUT', 'DELETE', 'OPTIONS'],
    allowedHeaders: ['Content-Type', 'Authorization', 'Accept', 'X-Requested-With'],
    credentials: true,
    maxAge: 3600
};
app.use(cors(corsOptions));

// Optional enforce HTTPS (behind proxy) — set ENFORCE_HTTPS=true in production env
if (ENFORCE_HTTPS) {
    app.enable('trust proxy'); // respect X-Forwarded-* headers
    app.use((req, res, next) => {
        const proto = req.headers['x-forwarded-proto'] || (req.protocol);
        if (req.secure || (typeof proto === 'string' && proto === 'https')) {
            return next();
        }
        // Reject non-HTTPS requests when enforce is on
        logger.warn('Rejected non-HTTPS request to secure server', { url: req.originalUrl, ip: req.ip });
        res.status(400).json({ message: 'Use HTTPS when connecting to this server' });
    });
}

// Rate limiting
const authLimiter = rateLimit({
    windowMs: AUTH_RATE_LIMIT_WINDOW_MS,
    max: AUTH_RATE_LIMIT_MAX,
    message: { message: 'Too many auth requests, please try again later.' }
});
const goalCompleteLimiter = rateLimit({
    windowMs: GOAL_COMPLETE_RATE_LIMIT_WINDOW_MS,
    max: GOAL_COMPLETE_RATE_LIMIT_MAX,
    message: { message: 'Too many goal-complete requests, slow down.' }
});

// Apply rate-limits to route prefixes
app.use('/api/auth', authLimiter);
// The path pattern with param isn't supported in app.use directly for limiter; middleware should be applied per-route.
// We'll also apply limiter in routes file for /goals/:id/complete if necessary.

// Mount API routes
app.use('/api', routes);

// Error handler must be last
app.use(errorHandler);

export default app;
