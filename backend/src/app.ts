// src/app.ts
import express from 'express';
import helmet from 'helmet';
import cors from 'cors';
import morgan from 'morgan';
import rateLimit from 'express-rate-limit';
import compression from 'compression';
import routes from './routes';
import { errorHandler } from './middleware/errorHandler';
import { AUTH_RATE_LIMIT_MAX, AUTH_RATE_LIMIT_WINDOW_MS, ALLOWED_ORIGINS, ENFORCE_HTTPS } from './config';
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
    methods: ['GET', 'POST', 'PUT', 'PATCH', 'DELETE', 'OPTIONS'],
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
app.use('/api/auth', authLimiter);

// Mount API routes
app.use('/api', routes);

// Error handler must be last
app.use(errorHandler);

export default app;
