-- Lua 脚本：滑动窗口限流
local key = KEYS[1]
local now = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local limit = tonumber(ARGV[3])

-- 1. 移除过期数据 (清理窗口外的请求)
redis.call('zremrangebyscore', key, 0, now - window * 1000)

-- 2. 统计当前窗口请求数
local count = redis.call('zcard', key)

-- 3. 判断是否超过阈值
if count < limit then
    redis.call('zadd', key, now, now)
    redis.call('expire', key, window) -- 设置过期时间，防止冷 Key 占用空间
    return 1
else
    return 0
end