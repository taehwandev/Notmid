export type NotmidRateLimitConfig = {
  maxRequests: number;
  windowMs: number;
};

export type NotmidRateLimitDecision = {
  allowed: boolean;
  limit: number;
  remaining: number;
  retryAfterSeconds: number;
  resetAt: number;
};

type RateLimitBucket = {
  count: number;
  resetAt: number;
};

export function createFixedWindowRateLimiter(config: NotmidRateLimitConfig) {
  const buckets = new Map<string, RateLimitBucket>();
  let checksSincePrune = 0;

  return {
    check(key: string, now: number = Date.now()): NotmidRateLimitDecision {
      pruneExpiredBuckets(buckets, now, checksSincePrune);
      checksSincePrune = (checksSincePrune + 1) % 256;

      const bucket = resolveBucket(buckets, key, now, config.windowMs);
      bucket.count += 1;

      const remaining = Math.max(config.maxRequests - bucket.count, 0);
      const retryAfterSeconds = Math.max(Math.ceil((bucket.resetAt - now) / 1000), 1);

      return {
        allowed: bucket.count <= config.maxRequests,
        limit: config.maxRequests,
        remaining,
        retryAfterSeconds,
        resetAt: bucket.resetAt,
      };
    },
  };
}

function resolveBucket(
  buckets: Map<string, RateLimitBucket>,
  key: string,
  now: number,
  windowMs: number,
): RateLimitBucket {
  const existing = buckets.get(key);

  if (existing && existing.resetAt > now) {
    return existing;
  }

  const bucket = {
    count: 0,
    resetAt: now + windowMs,
  };
  buckets.set(key, bucket);
  return bucket;
}

function pruneExpiredBuckets(
  buckets: Map<string, RateLimitBucket>,
  now: number,
  checksSincePrune: number,
): void {
  if (checksSincePrune !== 0) {
    return;
  }

  for (const [key, bucket] of buckets.entries()) {
    if (bucket.resetAt <= now) {
      buckets.delete(key);
    }
  }
}
