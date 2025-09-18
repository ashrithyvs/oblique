import Redis from 'ioredis';

class RedisClient {
  ttl: any;
  redisClient: any;
  rediesClientConnected: boolean;
  static instance: any;
  constructor(config) {
    const { host, port, ttl } = config;
    this.ttl = ttl;
    this.redisClient = new Redis.Cluster(
      process.env.REDIS_NODES
        ? RedisClient.#getClusterConfig(process.env.REDIS_NODES)
        : [
          {
            host,
            port
          }
        ],
      {
        slotsRefreshTimeout: 3000,
        enableOfflineQueue: false
      }
    );

    this.redisClient.on('error', (error) => {
      console.log('Redis Client Connection Error', error);
      this.rediesClientConnected = false;
      // TODO: error add to logger
    });
    this.redisClient.on('connect', () => {
      // TODO: connect message add to logger
    });
    this.redisClient.on('ready', () => {
      this.rediesClientConnected = true;
    });
  }

  static getInstance(config) {
    if (!RedisClient.instance) {
      RedisClient.instance = new RedisClient(config);
    }
    return RedisClient.instance;
  }

  static #getClusterConfig(redisNodes) {
    const nodes = redisNodes.split(' ');
    return nodes.map((node) => {
      const [host, port] = node.split(':');
      return { host, port };
    });
  }

  async set(key, value, ttl = this.ttl) {
    try {
      if (ttl > 0) {
        await this.redisClient.set(key, JSON.stringify(value), 'EX', ttl);
      } else {
        await this.redisClient.set(key, JSON.stringify(value));
      }
    } catch (error) {
      throw new Error(
        `Error occured while setting the key in redisCache : ${key}`,
        error.message
      );
    }
  }

  async mset(map, ttl = this.ttl) {
    try {
      await this.redisClient.mset(map, 'EX', ttl);
    } catch (error) {
      throw new Error(
        `Error occured while setting the key in redisCache : ${map}`,
        error.message
      );
    }
  }

  async get(key) {
    try {
      const getObj = await this.redisClient.get(key);
      const parsedGetObj = JSON.parse(getObj);
      return parsedGetObj;
    } catch (error) {
      throw new Error(
        `Error occured while getting the key from redisCache : ${key}`,
        error.message
      );
    }
  }

  async mget(keys) {
    try {
      const getObj = await this.redisClient.mget(keys);
      const mapObj = {};
      getObj.map((item, index) => {
        mapObj[keys[index]] = item ? JSON.parse(item) : item;
      });
      return mapObj;
    } catch (error) {
      throw new Error(
        `Error occured while getting the multiple-key from redisCache : ${keys}`,
        error.message
      );
    }
  }

  async hgetall(key) {
    try {
      const getObj = await this.redisClient.hgetall(key);
      if (
        Object.keys(getObj).length === 0 &&
        !(await this.redisClient.exists(key))
      )
        return null;
      return getObj;
    } catch (error) {
      throw new Error(
        `Error occured while getting the multiple-key from redisCache : ${error.message}`,
        error.message
      );
    }
  }

  async has(key) {
    try {
      return await this.redisClient.exists(key);
    } catch (error) {
      throw new Error(
        `Error occured while checking the key in redisCache : ${key}`,
        error.message
      );
    }
  }

  async del(key) {
    try {
      return await this.redisClient.del(key);
    } catch (error) {
      throw new Error(
        `Error occured while deleting the key in redisCache : ${key}`,
        error.message
      );
    }
  }

  async hset(key, map, ttl = this.ttl) {
    try {
      await this.redisClient.hset(key, map);
      if (ttl > 0) {
        await this.redisClient.expire(key, ttl);
      }
    } catch (error) {
      throw new Error(
        `Error occured while setting the key in redisCache : ${map}`,
        error.message
      );
    }
  }

  getStatus() {
    return this.rediesClientConnected;
  }
}

export const redisCache = {
  getInstance: RedisClient.getInstance
};
