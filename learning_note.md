# 📂 我的技术复盘笔记.md

## Redis

### 技术决策：为什么引入 Redis？

- **现状痛点**：首页看板数据涉及多表聚合计算（Stream API），随着数据量增加，数据库压力大且响应慢。
- **解决方案**：采用 Redis 作为二级缓存，利用 **Cache-Aside（旁路缓存）** 策略，将聚合后的 `Map` 数据整体缓存。
- **实际效果**：响应耗时从平均 150ms 降至 15ms，显著提升了用户体验。

#### Redis 缓存层深度实践

- **缓存模式**：Cache-Aside。
- **一致性策略**：先更新数据库，再清理缓存（Delete Pattern）。
- **异常防御**：
  - **穿透**：针对 Null 结果回写短期“空标记”。
  - **雪崩**：TTL = 基础时间 + 随机扰动值。
  - **降级**：Service 层读写操作包裹 `try-catch`，日志记录异常，逻辑返回数据库。

#### 1.核心依赖（必须添加）

```
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-redis</artifactId>
</dependency>

<dependency>
    <groupId>org.apache.commons</groupId>
    <artifactId>commons-pool2</artifactId>
</dependency>
```

------

为什么是这两个？

- **`spring-boot-starter-data-redis`**: 这是“全家桶”，包含了 `RedisTemplate` 和 `Jedis/Lettuce` 驱动。
- **`commons-pool2`**: 现在的 Spring Boot 默认使用 **Lettuce** 作为连接池，如果你在 `application.yml` 里配置了类似 `max-active`（最大连接数）的参数，就必须引入这个依赖，否则项目启动时会报错找不到连接池类。

#### 2. 配置场景：两种 Redis 配置的应用

- **场景 A：声明式缓存 (@Cacheable)**
  - **应用**：课程详情查询、用户信息查询。
  - **理由**：逻辑单一，直接使用 Spring 注解可以保持业务代码纯净，实现解耦。
- **场景 B：编程式缓存 (RedisTemplate)**
  - **应用**：首页看板聚合数据 (`/data` 接口)。
  - **理由**：数据结构复杂（Map 嵌套），且需要根据逻辑动态决定是否存入缓存，手动控制粒度更精细。

#### 3. 核心问题：序列化与过期策略

- **序列化决策**：统一使用 **Jackson2JsonRedisSerializer**。
  - *原因*：JSON 格式具备极高的可读性和跨平台通用性。
- **过期时间 (TTL) 策略**：
  - `recommend`: 15min（保证推荐算法的“鲜度”）。
  - `course`: 30min（平衡数据一致性与读取速度）。
  - `user`: 2h（降低非频繁变动数据的查询频率）。

#### **4. 缓存实施策略 (Caching Strategy)**

- **声明式缓存 (@Cacheable)**：应用于简单的实体查询接口。优点是开发效率高，代码侵入小。
- **编程式缓存 (RedisUtil)**：应用于复杂的算法推荐和多表看板统计。优点是控制粒度细，支持异步写入和复杂的 Key 失效逻辑。
- **一致性维护**：采用手动失效模式。在数据变更（更新/删除）后，调用 `redisUtil.deleteByPattern` 批量清理相关业务缓存，确保数据最终一致性。

### **LaissezFaireSubTypeValidator**与**BasicPolymorphicTypeValidator**的区别

#### 1. 核心区别对比

| **特性**              | **LaissezFaireSubTypeValidator**                            | **BasicPolymorphicTypeValidator**                      |
| --------------------- | ----------------------------------------------------------- | ------------------------------------------------------ |
| **设计哲学**          | **“无为而治”** (Laissez-faire 是法语“放任”的意思)           | **“白名单准入”**                                       |
| **安全性**            | **较低**。它不进行任何验证，允许序列化/反序列化几乎所有类。 | **较高**。开发者必须显式定义哪些类是允许被多态处理的。 |
| **SpringBoot 3 趋势** | 属于为了向后兼容而保留的方案。                              | **官方推荐方案**。强调通过限制基类来防范反序列化漏洞。 |

------

#### 2. 为什么要写 `builder.allowIfBaseType(Object.class)`？

这是 Jackson 为了解决著名的 **“反序列化漏洞”** 引入的机制。

- **风险场景**：如果 Redis 中存入了一个恶意的类名，当系统反序列化时，如果不做限制，Jackson 可能会实例化该恶意类并触发其内部代码（RCE 攻击）。
- **你的代码逻辑**：虽然你这里写的是 `Object.class`（依然比较宽松），但它建立了一个**显式的校验器**。在更严格的生产环境下，你可以把它改成 `allowIfBaseType("com.example.bishe.entity")`，这样 Jackson 就只允许反序列化你自己的实体类，安全性拉满。

------

#### 3. 在 3.x 版本中的表现

在 Spring Boot 3.x 环境下，Jackson 更加推崇**显式验证**。

- **方式 A (LaissezFaire)**：本质上是告诉 Jackson：“我信任所有的类，你随便转”。这在内网环境下没问题，但在安全审计时往往过不了关。
- **方式 B (BasicPolymorphicTypeValidator)**：这是“防御式编程”的体现。即使你现在允许的是 `Object.class`，它也为未来的细粒度权限控制预留了接口。

------

#### 4. 简历与面试话术（境界提升）

如果面试官问：“你配置 Jackson 时为什么要写这么复杂的 Validator？”

**你可以这样回答（绝对能体现出你对 3.x 版本的深入理解）：**

> “在 Spring Boot 3.x 的配置中，我放弃了传统的 `LaissezFaireSubTypeValidator`，转而使用 `BasicPolymorphicTypeValidator`。这是考虑到**多态反序列化的安全性**。通过构建验证器并指定 `allowIfBaseType`，可以有效防止恶意子类的非法实例化，规避潜在的反序列化远程代码执行漏洞。这不仅解决了数据转为 `LinkedHashMap` 的类型丢失问题，也符合现代 Web 开发的‘最小权限原则’。”

### RedisUtil



######  为什么需要这个工具类？

- **解耦底层 API**：如果未来从 `RedisTemplate` 换成其他的客户端，你只需要改 `RedisUtil` 一个地方，而不需要修改全系统的 Controller。
- **异常处理**：在工具类里统一进行 `try-catch`，防止因为 Redis 宕机导致整个业务接口直接崩溃（即所谓的“优雅降级”预留位）。
- **代码简洁性**：在 Controller 里，代码从 `redisTemplate.opsForValue().get(key)` 简化为 `redisUtil.get(key)`。

![image-20260109203209674](C:\Users\85346\AppData\Roaming\Typora\typora-user-images\image-20260109203209674.png)

### Q&A

#### 1. 为什么要用构造函数？

```java
//1.先配置好ObjectMapper
        ObjectMapper mapper = new ObjectMapper();
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
//关键：必须配置此项，否则反序列化会变LinkedHashMap
mapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);

//2.通过构造函数直接注入mapper
        Jackson2JsonRedisSerializer<Object>serializer 
= new Jackson2JsonRedisSerializer<>(mapper,Object.class);

```



- **不可变性 (Immutability)**：一旦 `serializer` 被创建，它的 `ObjectMapper` 就不再允许被修改，这避免了在多线程并发环境下可能出现的配置篡改风险。
- **线程安全 (Thread Safety)**：确保了序列化器在 Spring 容器中作为单例（Bean）存在时，其行为是高度一致且安全的。

#### **2. 为什么要继承 `CachingConfigurerSupport`？**

- **决策**：虽然不继承也能用，但继承它可以更规范地重写 `KeyGenerator` 等方法，使系统具备更好的扩展性。

#### **3. 序列化方案的选择：为什么是 Jackson？**

- **痛点**：JDK 默认序列化会让 Key 变成 `\xAC\xED...`，且无法跨语言。
- **方案**：采用 **Jackson2JsonRedisSerializer**。
- **境界说辞**：为了实现**数据透明化**。这样我们在生产环境排查问题时，直接用 `redis-cli` 就能读懂缓存内容，同时也方便未来可能存在的非 Java 微服务调用。

#### **4. 过期策略的艺术 (TTL)**

- **场景**：为什么不设为永久？
- **境界说辞**：**“缓存不是数据库的副本，而是热点的快照。”** * 推荐结果（15min）：算法是动态的，太久会导致推荐过时。
  - 用户信息（2h）：更新频率低，设久一点能极大减轻数据库 I/O 负担。
  - 差异化 TTL 还能有效防止**缓存雪崩**（避免所有缓存同时失效导致数据库瞬间崩溃）。

#### 5. 为什么要用构造函数？

- **不可变性 (Immutability)**：一旦 `serializer` 被创建，它的 `ObjectMapper` 就不再允许被修改，这避免了在多线程并发环境下可能出现的配置篡改风险。
- **线程安全 (Thread Safety)**：确保了序列化器在 Spring 容器中作为单例（Bean）存在时，其行为是高度一致且安全的。

### 避坑指南

#### 1.使用了 `selectBatchIds` 替代了循环中的 `selectById`。

这是一个非常重要的**面试加分点**，证明你考虑到了数据库连接开销。

如果 `topN` 是 20，这行代码会在循环里查 20 次数据库。

 **优化方案：** 在存入变量前，先 `.map(Map.Entry::getKey).collect(Collectors.toList())` 拿到所有 ID，然后用一行 `courseMapper.selectBatchIds(ids)` 查出所有课程。

![image-20260110204059559](C:\Users\85346\AppData\Roaming\Typora\typora-user-images\image-20260110204059559.png)

**技术点：缓存旁路模式 (Cache-Aside Pattern)**

- *实现*：手动实现“查询缓存 -> 缓存失效则查库 -> 回写缓存”的完整闭环。

**技术点：解决 N+1 查询问题**

- *优化*：在流式计算中，先提取 ID 集合，利用 MyBatis-Plus 的 `selectBatchIds` 进行一次性批量查询，显著降低了数据库 I/O 次数。

**技术点：数据一致性兜底**

- *策略*：在 Controller 层配合 `delPattern` 机制，在数据变更时主动失效相关缓存，确保了推荐结果的时效性。

#### 2.保持序列化统一

**“在处理分布式缓存时，最核心的教训是：永远保持序列化协议的单一本源（Single Source of Truth）。** 试图在工具类中通过正则表达式修复序列化报错，往往会破坏 Jackson 的字节流标识位（如 UTF-32 标记），导致更难排查的编码异常。正确的做法是：**配置一个统一的 ObjectMapper Bean，并让所有 Redis 组件（Template, Manager, Util）共享它。”**

#### 3. 序列化“身份不匹配”陷阱

- **现象**：出现 `Invalid UTF-32 character` 或反序列化得到的对象全是 `LinkedHashMap`。
- **教训**：项目里只能有**一个**统一的 `ObjectMapper` 配置。如果 `RedisTemplate` 和 `CacheManager` 使用的序列化器不一致，数据就会像“讲不同语言的人在交流”，彻底乱套。
- **金句**：永远不要试图用正则表达式去手动“清洗”Redis 里的 JSON 字符串，那是在破坏数据结构，应该从统一配置入手。

#### 4. Spring Cache 的“影子”调用

- **现象**：明明在 Service 加了 `@Cacheable`，但 Redis 里死活没数据，控制台一直打印 SQL。
- **教训**：**Spring 缓存是基于 AOP 切面的。** * 在 Controller 里必须调用 Service 自定义的方法，不能直接调 MyBatis-Plus 的 `list()`。
  - 在同一个类（ServiceImpl）内部方法互相调用，缓存注解也会失效。
- **金句**：外来的和尚好念经——只有从外部注入并调用的 Service 方法，才能被 Spring 拦截并加上缓存逻辑。

#### 5. SpEL 表达式的“单引号”惨案

- **现象**：所有用户的数据都串了，或者缓存 Key 变成了奇怪的字符串。
- **教训**：
  - `key = "#userId"`：动态取参数 `userId` 的值（正确）。
  - `key = "'#userId'"`：把整个 `#userId` 当成固定字符串（错误，会导致数据污染）。
- **金句**：单引号里是死数据，没有单引号才是活变量。

#### 6. 缓存一致性的“读写联动”

- **现象**：后台改了课程状态，前台页面刷新还是老数据。
- **教训**：**谁负责写数据，谁就负责删缓存。**
  - 查询接口（GET）：只负责 `@Cacheable`（读/存）。
  - 操作接口（POST/PUT/DELETE）：负责 `redisUtil.delPattern`（毁）。
- **金句**：宁可错杀一千（删掉整个前缀的缓存），不可留下一条脏数据（让用户看到错误内容）。



### 课程系统 Redis 缓存与一致性方案总结

#### 1. 核心架构设计

为了平衡首页的高并发访问和数据实时性，我们采用了 **Cache-Aside（旁路缓存）** 模式，并针对不同场景做了精细化处理。

#### 2. 关键技术点

| **技术点**       | **实现细节**                                                 | **解决的问题**                                               |
| ---------------- | ------------------------------------------------------------ | ------------------------------------------------------------ |
| **冗余字段同步** | 在评分表 `user_course_score` 冗余 `course_status` 字段。     | 避免首页统计时的多表 `JOIN` 查询，极大提升 SQL 性能。        |
| **事务一致性**   | 使用 `@Transactional` 确保课程表状态更新与评分表冗余字段更新同时成功或失败。 | 防止出现“课程已下架，但评分仍在统计”的数据不一致问题。       |
| **异步缓存清理** | 使用 `CacheCleanService` 配合 `@Async`，在数据库更新后异步清理 Redis。 | 缩短接口响应时间（RT），提升用户体验。                       |
| **缓存穿透防护** | 在 `getById` 中对不存在的 ID 缓存 `EMPTY_NODE` 标记。        | 防止黑客利用不存在的 ID 恶意攻击数据库。                     |
| **缓存雪崩防护** | 为每个 Key 的过期时间增加 `Random.nextInt(600)` 随机盐值。   | 防止大量缓存在同一时刻集体失效，造成数据库压力激增。         |
| **服务自动降级** | 在 Redis 读写位置增加 `try-catch` 日志拦截。                 | 确保 Redis 挂掉时，业务能自动切换到数据库查询，保证系统可用性。 |

##### Redis 异常捕获（Cache Error Handling）

在“毕设”级别，Redis 挂了程序跟着报错是正常的；但在“就业项目”级别，这叫**单点故障（Single Point of Failure）**。

**面试题模拟**：“如果你的 Redis 宕机了，整个系统就瘫痪了吗？” **你的标准答案**：“不，我实现了**缓存降级**。Redis 挂了，系统会自动去查数据库，保证业务不中断，只是响应时间稍微变长。”

###### 缓存一致性的深度思考

现在用了 `delPattern`，这叫“先改库，再删缓存”。

- **深度问题**：如果数据库改完了，删缓存失败了怎么办？
- **简历进阶点**：了解并尝试实现“延时双删”策略或监听 MySQL Binlog 同步 Redis（Canal 工具）。虽然毕设不需要真的写出来，但面试时能说出这个思路，档次立即不同。

##  **审计日志系统的深度优化**

 **重构系统审计日志模块，实现高性能异步审计体系**

1. 采用 AOP + 自定义注解实现业务逻辑与审计逻辑的零侵入解耦；
2. 引入 AsyncLogService 实现日志异步入库，消除 IO 操作对主业务性能的影响；
3. 优化序列化逻辑：手动过滤不可序列化对象（Request/Response），并实现长文本截断，防止数据库溢出。

- **为什么要异步？**
  - **现象**：同步写日志会让接口响应时间增加 30ms-50ms。
  - **方案**：利用 Spring `@Async` 将写库操作移出主线程。
  - **关键点**：必须在主线程完成 `BaseContext` 和 `Request` 数据的抓取，因为异步线程无法共享 `ThreadLocal` 变量。
- **序列化异常的处理（核心痛点）**：
  - **问题**：Jackson 序列化 `joinPoint.getArgs()` 时，若包含 `HttpServletRequest` 会报循环引用错误。
  - **解决**：编写 `processRequestParams` 方法，通过 `instanceof` 手动过滤系统级对象，只记录真正的业务 DTO。
- **数据安全与性能**：
  - 对 `oper_param` 和 `json_result` 进行 **字符串截断（substring）**，防止极端情况下大 JSON 撑爆数据库磁盘或导致 SQL 执行异常。

#### **💡 审计日志的“进阶细节”：**

1. **参数脱敏与过滤**：在 AOP 层手动剔除 `ServletRequest`、文件流等对象，规避 Jackson 循环引用导致的序列化崩溃。
2. **存储安全**：对请求参数和返回结果进行 **长度截断（Truncation）**，防止由于单条日志过大导致的数据库性能抖动或写入失败。
3. **真实 IP 溯源**：解析 `X-Forwarded-For` 请求头，解决 Nginx 反向代理环境下的用户真实 IP 丢失问题。

### **⚠ 避坑指南：**

#### **切面中的序列化陷阱**

- **问题描述**：在 AOP 中直接使用 Jackson 序列化 `joinPoint.getArgs()` 时，若参数包含 `HttpServletRequest`、`BindingResult` 等 Spring 内置对象，会导致 `JsonMappingException`。
- **对策**：在序列化前进行类型判断，**手动过滤**掉这些无法转为 JSON 的敏感/复杂对象。
- **经验总结**：日志记录应当“轻量化”，只记录核心业务 DTO，忽略系统级对象。

## 分布式

### 分布式滑动窗口限流

#### ZSet + Lua 的化学反应

1. **数据结构选型 (Why ZSet?)**：
   - 我们利用了 Redis 的 `ZSet`（有序集合）。我们将**时间戳**作为 `score`（分数），也作为 `value`。
   - **滑动原理**：传统的“固定窗口”在分钟切换时会有流量翻倍风险（例如 59 秒请求 10 次，01 秒又请求 10 次，虽然每分钟没超，但 2 秒内涌入 20 次）。
   - **滑动窗口解决方法**：每次请求进来，我们先执行 `zremrangebyscore`，把“当前时间减去窗口时间”之前的数据全部删掉。这样剩下的永远是最近一分钟内的活跃请求。
2. **原子性保障 (Why Lua?)**：
   - **挑战**：如果不使用 Lua，我们需要在 Java 里写：`获取次数 -> 判断 -> 增加次数`。在高并发下，两个请求可能同时判断为“次数未超”，然后同时写入，导致实际流量超标。
   - **解决**：Lua 脚本在 Redis 内部是 **单线程原子执行** 的。这意味着在脚本执行期间，不会有其他指令插队。这保证了限流计数的绝对准确，也就是**分布式一致性**。

3. **踩坑记录**：Lua 脚本返回的是 Long 类型，Java 接收时需注意 `setResultType(Long.class)`。

### **分布式并发控制——Redisson 实战**

#### 1. 技术方案：为什么 Redisson 替代 `synchronized`？

在 BOSS 直聘的 Java 岗要求中，“分布式锁”是考察你是否具备处理**集群并发问题**能力的分水岭。

| **维度**       | **synchronized (你目前的做法)** | **Redisson 分布式锁 (目标方案)**                          |
| -------------- | ------------------------------- | --------------------------------------------------------- |
| **生效范围**   | 仅当前 JVM（单台服务器）        | 整个 Redis 集群（所有服务器）                             |
| **锁失效风险** | 服务器宕机锁自动释放            | 支持 **Watch Dog（看门狗）** 机制，防止业务未完锁提前过期 |
| **等待机制**   | 线程阻塞，容易形成长队列        | 基于发布订阅机制，支持高并发下的自旋/等待，性能更优       |
| **就业竞争力** | 基础水平                        | **中高级/架构思维**                                       |

- **场景痛点**：推荐算法涉及全量评分计算、余弦相似度矩阵构建及多表组装，属于 **CPU/IO 双密集型操作**。若多用户同时触发冷启动或缓存失效，`synchronized` 在集群环境下无法阻挡多个计算任务同时涌入数据库。
- **解决方案 (DCL + Distributed Lock)**：
  - **双重检查锁 (Double-Check Locking)**：在加锁前后各进行一次缓存查询。第一次拦截 99% 的请求；第二次拦截在等锁期间由于前一个线程已完成计算并写入缓存的残余请求。
  - **原子性保障**：使用 Redisson 的 `tryLock` 替代本地锁，确保全网（分布式环境）同一时间只有一个线程在执行 `recommendationCalculation`。
- **容错处理**：
  - **超时机制**：设置 5 秒等待时长，若获取锁超时则直接返回空结果或兜底数据（HotCourse），避免线程堆积导致系统雪崩。
  - **看门狗 (Watch Dog)**：利用 Redisson 的自动续期机制，防止计算逻辑过长（如数据量突增时）导致锁意外释放。

## JMeter

| **工具**             | **学习成本**  | **真实感**                   | **就业竞争力**                       |
| -------------------- | ------------- | ---------------------------- | ------------------------------------ |
| **单元测试 (JUnit)** | 低            | 弱（模拟不出高并发）         | 基础要求                             |
| **Apache JMeter**    | **中 (推荐)** | **高（模拟多用户真实场景）** | **必修项**（证明你具备性能分析能力） |
| **Postman**          | 极低          | 无（只能单次触发）           | 不及格                               |

## 开发疑问

### 典型的 Spring Boot 工业级层级结构

基于你目前的代码，你的项目结构应该是这样的。我们要明确每一层**“只负责什么”**和**“坚决不做什么”**：

| **包名 (Package)** | **名称**     | **职责 (Role)**                                              | **举例**               |
| ------------------ | ------------ | ------------------------------------------------------------ | ---------------------- |
| **controller**     | 控制层       | **接线员**：接收请求、校验参数、调用 Service、返回结果。**不写业务逻辑。** | `CourseController`     |
| **service**        | 业务逻辑接口 | **合同**：定义业务要实现的功能。                             | `RecommendService`     |
| **service.impl**   | 业务逻辑实现 | **大脑**：核心逻辑、算法调用、**缓存控制**。                 | `RecommendServiceImpl` |
| **mapper / dao**   | 持久层       | **搬运工**：只负责执行 SQL。                                 | `CourseMapper`         |
| **entity / model** | 实体类       | **数据库映射**：对应数据库表的 Java 对象。                   | `Course.java`          |
| **dto / vo**       | 数据传输对象 | **外包装**：返回给前端的数据格式（可能合并了多个实体字段）。 | `Result.java` (R)      |
| **config**         | 配置类       | **环境搭建**：Redis、Swagger、MyBatisPlus 的全局配置。       | `RedisConfig`          |
| **utils**          | 工具类       | **万用工具**：纯逻辑封装，不带业务。                         | `RedisUtil`, `JwtUtil` |

 **你的业务逻辑：数据是怎么“跑”起来的？**

以你最核心的**“推荐热门课程”**为例，我们来看一个请求完整的生命周期：

1. **用户发起请求**：点击首页看板。
2. **Controller 接站**：`DashboardController` 收到请求，它不计算，而是喊一句：“`RecommendService`，请给我 10 门最热门的课”。
3. **Service 处理 (核心)**：
   - `RecommendServiceImpl` 收到指令。
   - **查缓存**：先问 `RedisUtil`：“缓存里有吗？”
   - **缓存命中**：直接还给 Controller。
   - **缓存未命中**：
     - 调用 `UserCourseScoreMapper` 查出所有评分。
     - 执行你的 **Stream 聚合算法**（计算平均分、排序）。
     - 调用 `CourseMapper.selectBatchIds` 补全课程详细信息。
     - 存入 Redis。
4. **结果返回**：数据层层打包成 `R.success(data)` 返回给前端展示。

### 业务逻辑与代码精简：四权分立架构

#### **1. 拦截器（JwtInterceptor）——负责保安工作**：

- 职责：验证 `Authorization` 头，解析 Token，拦截非法请求。
- **实现**：
  - `JwtInterceptor`：验证 Authorization 头，解析 Token
  - 传参机制：解析出的 `userId` 存入 `request.setAttribute("currUserId", userId)`
  - `WebMvcConfig`：配置 `excludePathPatterns` 实现动态权限开放（如登录、注册、公共课程列表）

#### **2. 实体类（Entity/Validation）—— 负责质检工作**

- **职责**：验证数据格式，保证数据质量
- **实现**：
  - 使用 `@NotBlank`、`@Min` 等注解进行字段校验
  - 配合 `@Valid` 注解在 Controller 层触发校验

#### **3. 服务层（Service）—— 负责核心工艺**

- **职责**：处理跨表事务、复杂逻辑及权限归属校验
- **实现**：
  - `getAndCheckTeacherAuth`：权限校验统一方法
  - 事务管理：`@Transactional` 注解保证数据一致性
  - 业务逻辑封装：复杂计算、算法实现

#### **4. 全局异常处理器（ExceptionHandler）—— 负责统一售后**

- **职责**：将所有异常转化为标准响应格式
- **实现**：
  - 统一处理 `RuntimeException` 和校验错误
  - 转换为标准 `R` 对象返回给前端
  - 优雅的错误信息展示

### **DTO与Entity转换：架构设计与实践总结**

#### **一、核心原则**

##### **1. 职责分离**

- **Entity**：数据持久层模型，对应数据库表结构
- **DTO**：数据传输对象，用于层间通信和接口交互
- **VO**：视图对象，用于前端展示（可视为特殊DTO）

##### **2. 转换目的**

- **数据脱敏**：隐藏敏感字段（密码、手机号等）
- **字段控制**：限制返回/接收的字段范围
- **格式适配**：前后端数据格式转换
- **性能优化**：避免不必要的字段传输

#### **二、转换模式对比**

| 模式            | 适用场景             | 优点                 | 缺点             |
| --------------- | -------------------- | -------------------- | ---------------- |
| **手动转换**    | 字段少、简单业务     | 无依赖、完全控制     | 代码冗余、维护难 |
| **MapStruct**   | 企业级应用、性能敏感 | 编译时生成、性能最好 | 需要学习配置     |
| **ModelMapper** | 快速原型、中小项目   | 简单易用、自动映射   | 性能较差、黑盒   |
| **BeanUtils**   | Spring项目、简单复制 | Spring内置、简单     | 属性名必须一致   |
| **框架投影**    | 查询优化场景         | 性能最好、直接映射   | 灵活性差         |

#### **三、实战决策树**

```
是否需要转换？
    ├─ 是完整对象转换？ → 使用MapStruct/ModelMapper
    │     ├─ 追求性能？ → MapStruct ✅
    │     ├─ 快速开发？ → ModelMapper/BeanUtils
    │     └─ 字段极少？ → 手动转换
    │
    ├─ 是查询条件构建？ → 直接使用字段（推荐）
    │     ├─ 简单条件 → LambdaQueryWrapper + DTO字段
    │     ├─ 复杂条件 → QueryWrapper + 动态构建
    │     └─ 复用查询 → 构建查询Builder类
    │
    └─ 是部分更新？ → 选择性映射
          ├─ 指定字段更新 → UpdateWrapper + DTO字段
          └─ 忽略null值 → 配置忽略策略
```

##### **路径一：完整对象转换**

```
需求：新增、详情返回等完整对象操作
    ├─ 追求性能？ → MapStruct（编译时生成，性能最优）
    ├─ 快速开发？ → ModelMapper/BeanUtils（简单易用）
    └─ 字段极少？ → 手动转换（无依赖，完全控制）
```



##### **路径二：查询条件构建**

```
需求：搜索、筛选等查询场景
    ├─ 简单条件 → LambdaQueryWrapper + DTO字段（类型安全）
    ├─ 复杂条件 → QueryWrapper + 动态构建（灵活性高）
    └─ 复用查询 → 构建查询Builder类（代码复用）
```



##### **路径三：部分字段更新**

```
需求：修改用户信息等部分更新
    ├─ 指定字段更新 → UpdateWrapper + DTO字段（精准控制）
    ├─ 忽略null值 → 配置忽略策略（自动过滤）
    └─ 全量更新 → 完整转换后更新（一致性保证）
```



##### **快速参考表**

| 场景             | 推荐方案 | 工具/技术                  | 优点               |
| ---------------- | -------- | -------------------------- | ------------------ |
| **新增实体**     | 完整转换 | MapStruct                  | 类型安全，性能好   |
| **查询列表**     | 条件构建 | LambdaQueryWrapper         | 直接使用字段，灵活 |
| **更新部分字段** | 指定更新 | UpdateWrapper              | 避免不必要字段传输 |
| **返回详情**     | 完整转换 | MapStruct + 自定义映射     | 脱敏，字段控制     |
| **复杂搜索**     | 动态构建 | QueryWrapper + Builder模式 | 支持复杂逻辑       |
| **微服务通信**   | 完整转换 | MapStruct + Protobuf       | 序列化效率高       |

##### **决策要点**

1. **问自己**：我真的需要转换整个对象吗？
2. **性能考虑**：数据量大时优先考虑查询优化
3. **维护成本**：长期项目优先类型安全方案
4. **团队习惯**：保持团队内部一致性

**黄金法则**：能直接用字段的就不要转换对象，必须转换对象时选择最合适的工具。



#### **四、代码规范建议**

##### **1. 分层转换策略**

```
// Controller层
@PostMapping("/users")
public Result<UserDTO> createUser(@Valid @RequestBody UserCreateDTO dto) {
    // DTO → Entity
    User user = UserConverter.INSTANCE.toEntity(dto);
    userService.save(user);
    // Entity → DTO
    return Result.success(UserConverter.INSTANCE.toDTO(user));
}

// Service层
public Page<UserDTO> queryUsers(UserQueryDTO queryDTO) {
    // 查询条件：直接使用DTO字段，不转换对象
    LambdaQueryWrapper<User> wrapper = buildQueryWrapper(queryDTO);
    Page<User> page = userService.page(new Page<>(queryDTO), wrapper);
    // 分页结果：批量转换
    return page.convert(UserConverter.INSTANCE::toDTO);
}
```



##### **2. 命名规范**

```
// 转换类命名
UserConverter / UserMapper / UserAssembler

// DTO命名
UserDTO                    // 通用DTO
UserCreateDTO / AddUserDTO // 创建DTO  
UserUpdateDTO / EditUserDTO // 更新DTO
UserQueryDTO / UserSearchDTO // 查询DTO
UserDetailDTO / UserInfoDTO // 详情DTO

// 方法命名
toEntity / fromDTO / convertToEntity
toDTO / toVO / convertToDTO
toQueryWrapper / toSpecification
```



##### **3. 包结构设计**

```
com.example.project
├── controller
│   └── UserController.java
├── service
│   └── UserService.java
├── domain
│   ├── entity
│   │   └── User.java
│   └── repository
│       └── UserRepository.java
├── dto
│   ├── request
│   │   ├── UserCreateDTO.java
│   │   ├── UserUpdateDTO.java
│   │   └── UserQueryDTO.java
│   └── response
│       ├── UserDTO.java
│       └── UserDetailDTO.java
└── converter
    └── UserConverter.java  // 或 mapper/assembler
```



#### **五、最佳实践总结**

##### **1. 该转换时转换（完整对象操作）**

```
// ✅ 新增：需要完整转换
public void addUser(UserCreateDTO dto) {
    User user = converter.toEntity(dto);
    repository.save(user);
}

// ✅ 返回详情：需要脱敏和格式控制
public UserDetailDTO getUserDetail(Long id) {
    User user = repository.findById(id);
    return converter.toDetailDTO(user); // 只返回必要字段
}
```



##### **2. 不该转换时不转换（查询和部分更新）**

```
// ✅ 查询：直接使用DTO字段构建条件
public List<User> query(UserQueryDTO dto) {
    return repository.list(new LambdaQueryWrapper<User>()
        .like(StringUtils.isNotBlank(dto.getName()), User::getName, dto.getName())
        .eq(dto.getStatus() != null, User::getStatus, dto.getStatus()));
}

// ✅ 部分更新：直接使用字段
public void updateEmail(Long id, String email) {
    repository.update(new LambdaUpdateWrapper<User>()
        .set(User::getEmail, email)
        .eq(User::getId, id));
}
```



##### **3. 性能优化要点**

```
// ✅ 批量转换
List<UserDTO> dtos = users.stream()
    .map(converter::toDTO)
    .collect(Collectors.toList());

// ✅ 分页转换
Page<UserDTO> dtoPage = userPage.convert(converter::toDTO);

// ✅ 查询时指定字段（避免select *）
LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
wrapper.select(User::getId, User::getName, User::getEmail);
```

##### 4.**LambdaQueryWrapper 和 QueryWrapper的区别**

###### ** 是 MyBatis-Plus 中构建查询条件的两种方式，它们是父子关系，但使用方式有重要区别。**

###### **优先使用 LambdaQueryWrapper 的情况：**

1. **字段引用**：99%的字段引用都应该用Lambda
2. **团队协作**：提高代码可维护性
3. **重构频繁**：项目初期或经常修改表结构
4. **新项目**：从一开始就建立好习惯

###### **使用 QueryWrapper 的情况：**

1. **动态字段名**：字段名来自变量或参数
2. **动态表名**：分表场景
3. **复杂SQL片段**：`apply()`方法
4. **函数调用**：数据库函数如`DATE(create_time)`
5. **子查询**：需要写子查询SQL时

#### **六、常见陷阱与解决方案**

##### **1. 循环依赖问题**

```
// ❌ 错误：User和Order互相引用
class User {
    List<Order> orders;
}
class Order {
    User user;
}

// ✅ 解决：使用DTO打破循环
class UserDTO {
    Long id;
    String name;
    List<OrderSimpleDTO> orders; // 简化版本
}
```



##### **2. 深拷贝与浅拷贝**

```
// 使用MapStruct的深拷贝配置
@Mapper
public interface UserConverter {
    @Mapping(target = "roles", ignore = true) // 避免自动拷贝集合
    UserDTO toDTO(User user);
    
    // 手动处理集合
    default UserDTO toDTOWithRoles(User user) {
        UserDTO dto = toDTO(user);
        dto.setRoles(roleConverter.toDTOList(user.getRoles()));
        return dto;
    }
}
```



##### **3. 空值处理策略**

```
// 配置全局空值策略
@Mapper(config = MappingConfig.class)
public interface UserConverter {
    @Mapping(target = "createTime", 
             expression = "java(entity.getCreateTime() != null ? entity.getCreateTime() : LocalDateTime.now())")
    UserDTO toDTO(User entity);
}
```



#### **七、技术选型推荐**

##### **中小项目快速开发**

```
Spring Boot + JPA/Hibernate
使用：BeanUtils.copyProperties() + 手动转换
适用：快速迭代，团队规模小
```



##### **企业级应用**

```
Spring Boot + MyBatis-Plus
使用：MapStruct（主） + LambdaQueryWrapper（查询）
适用：性能要求高，团队协作，长期维护
```



##### **微服务架构**

```
Spring Cloud + 各ORM框架
使用：MapStruct + 统一DTO规范
适用：服务间通信，API契约优先
```



#### **八、核心要点总结**

1. **转换目的**：不是为了转换而转换，而是为了解决具体问题
2. **按需转换**：查询条件直接用字段，完整对象才需要转换
3. **类型安全**：优先使用LambdaQueryWrapper，避免字符串硬编码
4. **性能意识**：批量操作、选择性字段、避免N+1查询
5. **代码清晰**：统一的命名和包结构，便于团队协作
6. **工具选择**：根据项目规模选择合适工具，不盲目追求技术

**转换的本质是"用数据创建新对象"**。只要没有创建新对象，只是使用DTO的字段值，就不算传统意义上的转换。

###### **一句话总结关键区别**

- **✅ 不转换**：直接使用DTO的**字段值**作为参数
- **❌ 转换**：用DTO的数据**创建新的Entity对象**



# 简历亮点描述：

## 个性化推荐系统

### 1. 缓存与性能优化

- **多维度 Redis 缓存策略**：针对首页高频访问场景，设计并实现了基于 **Cache-Aside** 模式的缓存方案。引入 **“空标记（EMPTY_NODE）”** 机制防止缓存穿透，并通过 **“随机过期盐值”** 解决缓存雪崩问题，确保了系统在极端压力下的稳定性。同时，通过性能压测和**EXPLAN**分析**SQL**执行计划，为username和评分符合条件等高频字段建立了**UNIQUE和复合索引**，将核心接口的查询效率提升到了const级别
- **柔性降级与故障容灾**：在缓存层实现了**自动读取/写入降级**逻辑。通过自定义CacheErrorHandler进行异常拦截，并配置了**快速失败（Fail-fast，500ms超时）**机制，确保在 Redis 故障时，业务能秒级无缝切换回数据库查询，保障了核心业务24/7不间断运行。

### 2. 数据一致性与高效对齐方案

- **高性能数据与一致性保障**：针对统计指标计算缓慢的问题，在评分表中引入 **`course_status` 冗余字段**。通过 **MyBatis-Plus 事务管理**及 **`@Async` 异步缓存清理**，在保证双表数据一致性的同时，将首页统计响应速度提升了数倍。整体上，我们遵循**“先更新数据库，再删除缓存”**的策略，并配合Spring的**@CacheEvict**注解，确保了缓存与数据库的最终一致性。

### 3. 系统架构重构：解耦、安全与优雅编码

- **声明式安全与鉴权架构**：基于 Spring Interceptor 实现了**统一鉴权切面**。将 Token 解析逻辑从 Controller 层完全剥离，结合 **ThreadLocal/Request 作用域** 传递用户信息，降低了业务模块间的耦合度。
- **零 Boolean/If 参数校验体系**：利用 JSR-303 (Hibernate Validator) 规范，通过 **`@Valid` 与 `@Validated`** 注解结合**全局异常处理器（GlobalExceptionHandler）**，实现了声明式的参数合法性校验，使 Controller 代码量精简 60% 以上。



## 简历技术点

### **Jackson 序列化机制**

- **现象回顾**：在没有 Redis 缓存时，Spring Boot 直接将对象转为 JSON 发给前端，这通常是**序列化**过程。Jackson 可以通过反射读取字段。
- **坑点真相**：当你从 Redis 读取数据时，涉及的是**反序列化**。Jackson 的默认行为是：
  1. 调用**无参构造器**实例化一个“空对象”。
  2. 通过反射调用 Setter 方法或直接操作字段，把从 Redis 拿到的 JSON 值塞进去。
- **面试标准回答**：如果不提供无参构造器，Jackson 就找不到“入口”来创建对象。虽然 Java 默认会提供一个，但如果你加了 `@AllArgsConstructor`（全参构造），默认的无参构造就会失效。
- **简历项目习惯**：所有的 DTO、VO、POJO 建议都加上 `@Data`、`@NoArgsConstructor` 和 `@AllArgsConstructor`，这是**企业级开发的基本规约**，保证了在各种序列化框架（Jackson、Hessian、Kryo）下的兼容性。

### **复杂业务下的查询性能优化**

- **N+1 性能优化**：针对关联表查询场景，设计并实现了基于 **“批量映射+内存组装”** 的查询策略。通过 Map 结构对关联数据进行预缓存组装，**规避了 SQL 长连接占用和频繁的数据库 IO 交互**，接口响应效率提升明显。
- **无感身份鉴权**：重构鉴权模块，利用 **JWT 结合 ThreadLocal** 封装统一上下文对象（BaseContext），实现了用户信息在 Controller、Service 层间的透明传递，提升了代码的整洁度与安全性。



### 企业级审计日志与监控体系构建

#### 1. 架构设计：非侵入式切面审计

- **核心技术**：基于 **Spring AOP + 自定义注解**，实现业务逻辑与审计逻辑的物理解耦。
- **健壮性处理**：
  - **上下文透传**：解决异步场景下 `SecurityContext` 丢失问题。
  - **流量裁剪**：对大数据量字段实现**智能过滤与长度自适应截断**，防止日志库爆炸。

#### 2. 性能优化：异步解耦与背压保护

- **异步机制**：采用 `ThreadPoolTaskExecutor` + `@Async` 实现主线程“零等待”。
- **资源隔离**：通过**独立线程池**配置，防止日志 I/O 耗时波动影响核心业务稳定性。
- **背压信号（Backpressure）**：显式配置 `CallerRunsPolicy`，在极端高并发下强制主线程介入，利用响应延迟平滑抑制流量洪峰。

#### 3. 声明式校验与审计联动机制

- **校验前置**：利用 **JSR-303 标准** 实现声明式校验，将合规性检查逻辑从业务代码中彻底剥离。
- **异常捕获闭环**：通过 AOP 环绕通知深度捕获 `ConstraintViolationException`。**即使请求因校验失败未触发 Service，审计系统仍能完整记录非法请求轨迹（IP、参数、报错原因）**。
- **统一反馈**：结合 `GlobalExceptionHandler` 实现异常归口处理，保障前端交互的一致性。

#### 4. 监控与容灾：高可用保障方案

- **动态监控**：基于守护线程实时追踪线程池活跃度及队列堆积，实现任务流向可视化。
- **优雅停机**：配置 `setWaitForTasksToCompleteOnShutdown`，解决进程重启时内存日志丢失的“最后一公里”问题。
- **二级容灾**：设计 **DB + Redis 双重降级方案**。在数据库宕机或连接池爆满时自动切换至 Redis 暂存，确保审计数据“零丢失”。

#### 避坑指南：

##### 1. 异步上下文丢失坑 (SecurityContext & RequestContext)

**问题：** 异步线程执行时，`@Async` 会新开线程，导致 `ThreadLocal` 里的数据（如当前登录用户信息、HTTP Header）全部丢失。

- **后果：** 日志里记录的操作人全是 `null`，或者拿不到请求的 `IP` 地址。
- **避坑：** * **方案 A：** 在调用异步方法前，手动提取用户信息作为参数传递。
  - **方案 B（推荐）：** 自定义 `TaskDecorator`。在线程切换时，将 `SecurityContext` 或 `ServletRequestAttributes` 从主线程拷贝到子线程。

##### 2. 循环调用失效坑 (Self-Invocation)

**问题：** 在同一个 Service 类中，方法 A 调用带 `@Async` 的方法 B。

- **后果：** **异步不生效！** 方法 B 依然在主线程同步执行。
- **原因：** Spring 的异步是基于代理的，内部调用绕过了代理对象。
- **避坑：** * 将异步方法抽离到独立的 `AsyncService` 中。
  - 或者通过 `ObjectProvider` 或 `AopContext.currentProxy()` 注入自己。

##### 3. 序列化与大字段坑 (Data Explosion)

**问题：** 日志 AOP 拦截所有请求参数并转成 JSON 存入 DB。

- **后果：** 如果前端传了个 **Base64 图片**或**大文件二进制**，数据库单条记录会瞬间达到几 MB，直接撑爆磁盘或导致插入极慢。
- **避坑：**
  - **智能截断：** 在 AOP 层通过反射判断参数类型，对超过 2000 字符的字符串进行截断。
  - **黑名单：** 排除掉敏感接口（如上传接口、修改头像接口）的 `body` 记录。

##### 4. 优雅关闭失效坑 (Zombie Tasks)

**问题：** 强制重启服务器（`kill -9`）或直接关闭容器。

- **后果：** 线程池队列里积压的几百条审计日志直接丢失。
- **避坑：**
  - 配置 `setWaitForTasksToCompleteOnShutdown(true)`。
  - 设置 `setAwaitTerminationSeconds(60)`，给线程池最多 60 秒时间处理“临终遗言”。

##### 5. Redis 补偿风暴坑 (Recovery Storm)

**问题：** 当 DB 恢复后，要把 Redis 积压的日志刷回 DB。

- **后果：** 如果积压过多，一次性大量写入会导致数据库再次崩溃（二次雪崩）。
- **避坑：**
  - **平滑迁移：** 使用定时任务（如 XXL-JOB），每分钟从 Redis 弹出 100 条记录缓慢写入 DB，而不是一次性全部取出。

![image-20260119184642962](C:\Users\85346\AppData\Roaming\Typora\typora-user-images\image-20260119184642962.png)



###  接口方案与鉴权说明（已转换格式）

- **接口文档**：项目使用 Knife4j v4.3.0 集成 SpringDoc-OpenAPI，自动生成功能文档。****
  - 访问地址：http://localhost:8080/doc.html
- **鉴权机制（JWT）**：采用标准 HTTP Bearer 方案：
  - Header 字段：Authorization
  - 格式：Bearer {JWT_TOKEN}
- **调试步骤**（Spring Boot 3 兼容模式）
  - 调用 /api/login 获取 Token。
  - 在 doc.html 页面点击左侧「Authorize」按钮，输入框中手动添加 Bearer 前缀后填入完整 Token。
  - 调试需权限接口（如课程评分）时，JwtInterceptor 会自动解析 Token 并将用户 ID 写入 BaseContext。

### 分布式滑动窗口限流

#### ZSet + Lua 的化学反应

1. **数据结构选型 (Why ZSet?)**：
   - 我们利用了 Redis 的 `ZSet`（有序集合）。我们将**时间戳**作为 `score`（分数），也作为 `value`。
   - **滑动原理**：传统的“固定窗口”在分钟切换时会有流量翻倍风险（例如 59 秒请求 10 次，01 秒又请求 10 次，虽然每分钟没超，但 2 秒内涌入 20 次）。
   - **滑动窗口解决方法**：每次请求进来，我们先执行 `zremrangebyscore`，把“当前时间减去窗口时间”之前的数据全部删掉。这样剩下的永远是最近一分钟内的活跃请求。
2. **原子性保障 (Why Lua?)**：
   - **挑战**：如果不使用 Lua，我们需要在 Java 里写：`获取次数 -> 判断 -> 增加次数`。在高并发下，两个请求可能同时判断为“次数未超”，然后同时写入，导致实际流量超标。
   - **解决**：Lua 脚本在 Redis 内部是 **单线程原子执行** 的。这意味着在脚本执行期间，不会有其他指令插队。这保证了限流计数的绝对准确，也就是**分布式一致性**。

3. **踩坑记录**：Lua 脚本返回的是 Long 类型，Java 接收时需注意 `setResultType(Long.class)`。

### **分布式并发控制——Redisson 实战**

- **场景痛点**：推荐算法涉及全量评分计算、余弦相似度矩阵构建及多表组装，属于 **CPU/IO 双密集型操作**。若多用户同时触发冷启动或缓存失效，`synchronized` 在集群环境下无法阻挡多个计算任务同时涌入数据库。
- **解决方案 (DCL + Distributed Lock)**：
  - **双重检查锁 (Double-Check Locking)**：在加锁前后各进行一次缓存查询。第一次拦截 99% 的请求；第二次拦截在等锁期间由于前一个线程已完成计算并写入缓存的残余请求。
  - **原子性保障**：使用 Redisson 的 `tryLock` 替代本地锁，确保全网（分布式环境）同一时间只有一个线程在执行 `recommendationCalculation`。
- **容错处理**：
  - **超时机制**：设置 5 秒等待时长，若获取锁超时则直接返回空结果或兜底数据（HotCourse），避免线程堆积导致系统雪崩。
  - **看门狗 (Watch Dog)**：利用 Redisson 的自动续期机制，防止计算逻辑过长（如数据量突增时）导致锁意外释放。

## 简历重构

> **【原描述】**：实现了课程评分功能。 

**【就业级描述】**：

- **分布式 API 防护**：针对核心业务接口（如课程评分），设计并实现了基于 **Redis ZSet + Lua 脚本** 的分布式滑动窗口限流器。
- **技术深度**：利用 Lua 脚本解决了分布式环境下“读-写”操作的**竞态条件（Race Condition）**，确保了限流逻辑的**原子性**。
- **架构优势**：通过 **AOP 结合自定义注解**实现限流逻辑与业务逻辑的零侵入解耦；采用 **滑动窗口算法** 替代传统固定窗口，成功消除了窗口临界点突发流量绕过防护的安全隐患，支持按 UserID/IP 多维度灵活配置。
- **性能表现**：在压测场景下，限流逻辑在 Redis 端的执行时间仅为 **毫秒级**，对接口主业务性能损耗几乎忽略不计。



> **【原描述】**：分布式锁的引用

- **分布式并发治理**：针对计算密集型的协同过滤算法接口，设计并实现了 **“分布式锁 + 双重检查锁（DCL）”** 优化方案，彻底解决了高并发场景下的**缓存击穿（Cache Breakdown）**问题。
- **核心技术**：利用 **Redisson** 的 `RLock` 结合 `tryLock` 等待机制，确保了分布式集群环境下复杂计算逻辑的**强原子性**，将算法触发频率从“并发触发”降低为“全局唯一触发”。
- **高可用保障**：引入**缓存空列表（Null Object）**策略防止缓存穿透；通过 **try-lock 超时降级** 逻辑提升了系统的响应韧性，确保在极端拥塞情况下服务不挂死。

## 面试场景



> #### 你既然用了 Spring Cache 注解，怎么处理缓存不可用的情况？
>

**你**：我在项目中针对不同场景采用了两种策略：

1. 对于**通用查询**（如首页数据），我通过自定义 `CacheErrorHandler` 拦截框架异常，实现 AOP 层面的**透明降级**，确保 Redis 故障时业务不中断。
2. 对于**高频且复杂的详情查询**（如 `getById`），我采用了**手动双检（Manual Check）**。这不仅是为了实现随机盐过期和空对象存储（防穿透），更是为了在代码级别实现**精细化的异常隔离**。即使 Redis 服务完全不可用，`getById` 也能通过 `try-catch` 逻辑在 500ms 内快速切换到数据库模式。



> #### 如果评分表有 100 万条数据，你这一句 `selectList(null)` 就会导致 OOM（内存溢出）。 
>

- **SQL 优化**：不查询 `select *`，只查询 `id, user_id, course_id, score` 这几个必要的字段。
- **异步计算**：首页数据可以由一个 **定时任务（Quartz 或 Spring Task）** 每隔 10 分钟计算一次并存入 Redis。而 `getDashboardData` 接口 **只读 Redis**。如果 Redis 没数据，再触发实时计算。



> #### 如果数据库宕机了，你的接口返回 200 OK 加上空列表，前端会以为真的没有数据，这合理吗？
>

**专业回答**：不合理。缓存报错应该“静默降级”（因为有数据库保底），但**数据库/核心业务报错应该“显式抛出”**。这样监控系统才能发现数据库挂了，前端也能给用户展示“系统维护中”而不是“暂无数据”。



> #### “你在做日志切面时，如果接口参数里有 HttpServletRequest，Jackson 报错了你怎么处理？”

**对策**：在序列化前进行类型判断，**手动过滤**掉这些无法转为 JSON 的敏感/复杂对象。



> #### **“既然是审计日志，如果你的异步线程池满了，触发了 CallerRunsPolicy（由主线程执行），会导致你的业务接口响应变慢吗？”**

**满分回答：** “会。这正是我们设计的**背压（Backpressure）**机制。如果日志系统处理不过来，说明当前负载过高，通过让主线程执行日志写入，可以变相地限制前端请求的进入速度，保护底层数据库不被瞬间压垮。这比直接抛弃日志（AbortPolicy）更安全，比无限制堆积队列（导致 OOM）更稳健。”



> #### **既然 AOP 已经记了日志，为什么还要抛出异常给 GlobalExceptionHandler？**”

**你的核心回答点：**

- **职责分离**：AOP 负责的是**“审计与存证”**（记录发生了什么）；GlobalExceptionHandler 负责的是**“对外协议”**（告诉前端发生了什么）。
- **事务回滚**：如果是 Service 内部抛出的异常，AOP 必须抛出异常，Spring 的 `@Transactional` 才能感知到并触发回滚。如果不抛出，事务会以为执行成功并提交。

分布式滑动窗口限流器：

> **你为什么要用 ZSet 而不是简单的 String 自增（INCR）？**

- **答**：INCR 是固定窗口，无法处理窗口临界点的突发流量翻倍问题；ZSet 可以精准记录每个请求的时间戳，实现真正的滑动窗口，防护更严密。

> **Lua 脚本如果执行报错了，会影响你的业务吗？**

- **答**：我在 AOP 中捕获了 Redis 相关的异常。在生产环境下，如果限流器挂了，我们应该选择**“容错转发”**（让请求过去），而不是让系统卡死。

> **如果现在要改成“按 IP 限流”，你的代码需要改哪里？**

- **答**：不需要改逻辑。只需要在 `@RateLimit` 注解中设置 `limitType = LimitType.IP`。我的 AOP 切面会识别这个参数，并自动从 `ServletRequestAttributes` 中获取客户端真实 IP 作为 Redis 的 Key。