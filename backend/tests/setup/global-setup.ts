import { MongoMemoryReplSet } from 'mongodb-memory-server';
import fs from 'fs';
import path from 'path';

const tmpFile = path.join(__dirname, 'mongoUri.json');

export default async function globalSetup() {
    const replSet = await MongoMemoryReplSet.create({ replSet: { count: 1 } });
    const uri = replSet.getUri();
    // persist URI to disk so tests can pick it up
    fs.writeFileSync(tmpFile, JSON.stringify({ uri }), { encoding: 'utf8' });
    // also store pid so teardown can stop the server
    (global as any).__MONGO_REPLSET__ = replSet;
}
