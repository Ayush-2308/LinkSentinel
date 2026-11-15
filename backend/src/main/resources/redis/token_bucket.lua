-- Atomic token bucket. Same steps as TokenBucket.consume:
-- available = min(capacity, tokens + elapsedSeconds * refill), then spend 1 or compute retry.
local capacity = tonumber(ARGV[1])
local refill = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local ttl = tonumber(ARGV[4])

local data = redis.call('HMGET', KEYS[1], 'tokens', 'ts')
local tokens = tonumber(data[1])
local ts = tonumber(data[2])

if tokens == nil then
  tokens = capacity
  ts = now
end

local elapsed = now - ts
if elapsed < 0 then
  elapsed = 0
end

local available = math.min(capacity, tokens + (elapsed / 1000.0) * refill)
local allowed = 0
local retry = 0

if available >= 1 then
  available = available - 1
  allowed = 1
else
  retry = math.ceil((1 - available) / refill)
  if retry < 1 then
    retry = 1
  end
end

redis.call('HSET', KEYS[1], 'tokens', tostring(available), 'ts', tostring(now))
redis.call('EXPIRE', KEYS[1], ttl)
return allowed .. ':' .. retry
