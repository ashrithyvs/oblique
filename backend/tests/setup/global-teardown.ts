import fs from 'fs';
import path from 'path';

const tmpFile = path.join(__dirname, 'mongoUri.json');

export default async function globalTeardown() {
    try {
        // remove file
        if (fs.existsSync(tmpFile)) fs.unlinkSync(tmpFile);
    } catch (err) {
        // ignore
    }
}
