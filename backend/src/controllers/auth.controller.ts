// src/controllers/auth.controller.ts
import { Request, Response } from 'express';
import * as userSvc from '../services/user.service';
import { signJwt } from '../utils/jwt';
import { registerSchema, loginSchema } from '../utils/validators';

export async function register(req: Request, res: Response) {
    try {
        const parsed = registerSchema.parse(req.body);
        console.log("Parsed register body:", parsed);

        if (parsed.email) {
            const existing = await userSvc.findUserByEmail(parsed.email);
            if (existing) return res.status(400).json({ message: 'Email already used' });
        }

        const user = await userSvc.createUser({
            email: parsed.email,
            name: parsed.name,
            pin: parsed.pin,
            password: parsed.password
        });

        const token = signJwt({ sub: user._id, email: user.email });
        return res.json({ token, user: { id: user._id, email: user.email, name: user.name } });
    } catch (err: any) {
        console.error('register error', err);
        return res.status(400).json({ message: err?.message || 'Invalid payload' });
    }
}

export async function loginByEmail(req: Request, res: Response) {
    try {
        const parsed = loginSchema.parse(req.body);

        const email = parsed.email;
        const password = (req.body as any).password;

        // Try password login first if password provided
        if (password) {
            const user = await userSvc.verifyUserPasswordByEmail(email, password);
            if (!user) return res.status(401).json({ message: 'Invalid credentials' });
            const token = signJwt({ sub: user._id.toString(), email: user.email });
            return res.json({ token, user: { id: user._id, email: user.email, name: user.name } });
        }

        return res.status(400).json({ message: 'Missing credentials. Provide password.' });
    } catch (err: any) {
        console.error('login error', err);
        return res.status(400).json({ message: err?.message || 'Invalid payload' });
    }
}
