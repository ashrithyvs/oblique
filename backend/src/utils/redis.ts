import Redis, { Redis as RedisClient } from "ioredis";
import { logger } from "./logger";

interface SetRedisKeyParams {
  _id: string;
  data: any;
  expTime?: number;
}

class RedisService {
  private static instance: RedisService;
  private redis: RedisClient | null = null;

  private constructor() {}

  public static getInstance(): RedisService {
    if (!RedisService.instance) {
      RedisService.instance = new RedisService();
    }
    return RedisService.instance;
  }

  public async init(): Promise<void> {
    try {
      console.log(
        "REDIS_URL_INIT",
        process.env.REDIS_HOST,
        process.env.REDIS_PORT,
        process.env.REDIS_PASS
      );

      this.redis = new Redis({
        host: process.env.REDIS_HOST || "",
        port: Number(process.env.REDIS_PORT) || 6378,
        password: process.env.REDIS_PASS,
        connectTimeout: 10000,
        retryStrategy: (times: number) => {
          if (times >= 10) {
            console.error("[Redis] Retry limit reached. No further attempts.");
            return null;
          }
          const delay = Math.min(times * 50, 2000);
          console.error(`[Redis] Retrying connection in ${delay}ms...`);
          return delay;
        },
        maxRetriesPerRequest: 3,
        enableOfflineQueue: false,
      });

      this.redis.on("connect", () => {
        console.log("[Redis] Connected successfully.");
      });

      this.redis.on("ready", () => {
        console.log("[Redis] Ready to accept commands.");
      });

      this.redis.on("error", (err: Error) => {
        console.error("[Redis] Error:", err.message);
      });

      this.redis.on("end", () => {
        console.error("[Redis] Connection closed.");
      });

      this.redis.on("reconnecting", (delay: number) => {
        console.error(`[Redis] Reconnecting in ${delay}ms...`);
      });
    } catch (e: any) {
      console.error("[Redis] Initialization Error:", e.message || e);
    }
  }

  public getClient(): RedisClient | null {
    return this.redis;
  }

  public async setRedisKey({ _id, data, expTime = 180 }: SetRedisKeyParams): Promise<void> {
    try {
      await this.executeRedisCommand("set", _id, JSON.stringify(data), "EX", expTime.toString());
    } catch (err: any) {
      console.error("[Redis] Error in setRedisKey:", err.message);
    }
  }

  public async getRedisKey(key: string): Promise<string | null> {
    try {
      return await this.executeRedisCommand("get", key);
    } catch (err: any) {
      console.error("[Redis] Error in getRedisKey:", err.message);
      return null;
    }
  }

  public async removeRedisKey(key: string): Promise<void> {
    try {
      await this.executeRedisCommand("del", key);
    } catch (err: any) {
      console.error("[Redis] Error in removeRedisKey:", err.message);
    }
  }

  private async executeRedisCommand(command: string, ...args: any[]): Promise<any> {
    try {
      if (!this.redis || this.redis.status !== "ready") {
        logger.error(`[Redis] Skipping command "${command}": Redis is not connected.`);
        return null;
      }
      await this.redis.select(0);
      return (this.redis as any)[command](...args);
    } catch (err: any) {
      console.error(`[Redis] Error executing "${command}":`, err.message);
      return null;
    }
  }

  public async deletePrefixKey(prefix: string) {
    try {
      if (!this.redis || this.redis.status !== "ready") {
        logger.error(`[Redis] Skipping delete with prefix "${prefix}": Redis is not connected.`);
        return;
      }
  
      await this.redis.select(0);
      const stream = this.redis.scanStream({ match: `${prefix}*`, count: 100 });
      let localKeys: string[] = [];
      let pipeline = this.redis.pipeline();
      
      stream.on('data', function (resultKeys) {
        for (var i = 0; i < resultKeys.length; i++) {
          localKeys.push(resultKeys[i]);
          pipeline.del(resultKeys[i]);
        }
        if(localKeys.length > 100){
          pipeline.exec(()=>{console.log("one batch delete complete")});
          localKeys=[];
          pipeline = this.redis.pipeline();
        }
      });

      await new Promise((resolve, reject) => {
        stream.on("end", resolve);
        stream.on("error", reject);
      });
  
      await pipeline.exec();
      logger.info(`[Redis] Deleted keys with prefix "${prefix}"`);
    } catch (err: any) {
      console.error(`[Redis] Error deleting keys with prefix "${prefix}":`, err.message);
    }
    
  }
}

const redisService = RedisService.getInstance();
const redis = redisService.getClient();

export {
  redis,
  redisService,
  RedisService
};
