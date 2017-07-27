# Spring Cloud Zuul 笔记

# 简介
> Spring Cloud Zuul 基于 Netflix Zuul 实现，整合了 Spring Cloud Ribbon、 Spring Cloud Hystrix 和 Spring Cloud Actuator。
Ribbon 用来实现在网关服务进行路由转发时候的客户端负载均衡以及请求重试。Hystrix 用来在网关服务中实现对微服务转发时的保护机制，通过线程隔离
和断路器，防止微服务的故障引发 API 网关资源的无法释放，从而影响其它应用的对外服务。Spring Cloud Zuul 解决了两个问题：
> 1.  对于路由规则与服务实例的维护问题；
> 2.  对于类似签名校验、登陆校验在微服务架构中的冗余问题。

# 使用步骤
* pom 中引入 spring-cloud-starter-eureka 和 spring-cloud-starter-zuul 的依赖
* 主类上加 @EnableZuulProxy 注解开启 Zuul 的 API 网关服务
* 在 application.properties 中配置 Zuul 应用的基础信息，如：应用名、服务端口等

# 传统路由（面向IP）
* 单实例路由规则：zuul.routes.\<route\>.path，zuul.routes.\<route\>.url，如：
> 1.  zuul.routes.api-a-url.path=/api-a-url/**
> 2.  zuul.routes.api-a-url.url=http://localhost:8080/
* 多实例路由规则及配置：zuul.routes.\<route\>.path，zuul.routes.\<route\>.serviceId，\<route\>.ribbon.listOfServers，如；：
> 1.  zuul.routes.user-service.path/user-service/**，      
> 2.  zuul.routes.user-service.serviceId=user-service，
> 3.  ribbon.eureka.enabled=false，       
> 4.  user-service.ribbon.listOfServers=http://localhost:8081/,http://localhost:8080/  
> 5.  这里使用 ribbon.eureka.enabled=false 关闭 Ribbon 根据服务发现机制来获取配置服务名对应的实例清单机制，否则配置的 serviceId 获取不到
对应实例的清单。
> \<route\>.ribbon.listOfServers 配置指定了具体的实例清单。

# 服务路由（面向服务），需先使用@EnableDiscoveryClient注解开启服务发现
* 路由规则方式一：zuul.routes.\<route\>.path，zuul.routes.\<route\>.serviceId，如：
> 1.  zuul.routes.user-service.path=/user-service/**，  
> 2.  zuul.routes.user-service.serviceId=user-service
* 路由规则方式二：zuul.routes.\<route\>，其中 \<route\> 就是服务名，如：
> zuul.routes.user-service=/user-service/**

# 请求过滤
> 如果不使用 zuul 对路由进行过滤从而实现权限验证，最简单粗暴的方法就是为每个微服务应用都实现一套用于校验签名和鉴别权限的过滤器或拦截器，
不过这样并不可取，它会增加以后系统的维护难度，造成代理冗余。所以，比较好的做法就是将这些校验逻辑剥离出去，构建一个独立的鉴权服务，但这样的
做法仅仅只是解决了鉴权逻辑的分离，并没有在本质上将这部分不属于冗余的逻辑从原有的微服务应用中拆分处，冗余的拦截器或过滤器依然存在。
> 更好的做法是通过前置网关服务来完成这些非业务性质的校验。由于网关服务的加入，外部客户端访问我们的系统已经有了统一的入口，同时，通过在网关中
完成校验和过滤，微服务应用端就可以去除各种复杂的过滤器和拦截器了，这使得微服务应用接口的开发和测试复杂度也得到了相应降低。
* 实现步骤
> 1.  实现继承 ZuulFilter 的自定义拦截器，各方法作用如下：
>> filterType：过滤器类型，它决定过滤器在请求的哪个生命周期中执行。pre：可以在请求被路由之前调用；routing：在路由请求时被调用；post：在
routing 和 error 过滤器之后被调用；error：处理请求时发生错误时被调用。
>> filterOrder：过滤器的执行顺序。当请求在一个阶段中存在多个过滤器时，需要根据该方法返回的值来依次执行，数值越小优先级越高。
>> shouldFilter：判断该过滤器是否需要被执行。实际运用中，可以利用该函数来执指定过滤器的有效范围。
>> run：过滤器的具体逻辑。可实现自定义的过滤逻辑，来确定是否要拦截当前的请求，不对其进行后续的路由（requestContext.setSendZuulResponse(false)），
或是在请求路由返回结果之后，对处理结果做一些加工等。
> 2.  加入该 XxxFilter 的 Bean 创建，可以在主类中，也可以结合 @Configuration 和 @Bean 来创建
> 3.  参考 AccessFilter，对所有的请求必须有 accessToken 才进行转发，否则拦截该请求，如：
>> http://localhost:5555/api-a-url/hello1?accessToken=test-token&name=%E6%9D%8E%E5%9B%9B
>> http://localhost:5555/api-a/hello1?accessToken=test-token&name=%E6%9D%8E%E5%9B%9B
>> http://localhost:5555/api-a2/hello1?accessToken=test-token&name=%E7%8E%8B%E4%BA%94
* 使用 zuul 的原因及作用总结
> 1.  它作为系统的统一入口，屏蔽了系统内部各个微服务的细节
> 2.  它可以与服务治理框架的结合，实现自动化的服务实例维护以及负载均衡的路由转发
> 3.  它可以实现接口权限校验与微服务业务逻辑的解耦
> 4.  通过服务网关中的过滤器，在各生命周期中去校验请求的内容，将原本在对外服务层做的校验前移，保证了微服务的无状态性，同时降低了微服务测试
难度，让服务本身更集中关注业务逻辑的处理

# 服务路由的默认规则
> 当为 Spring Cloud Zuul 构建的 API 网关服务引入 Spring Eureka 之后，它为 Eureka 中的每个服务都自动创建一个默认路由规则，这些默认规则
的 path 会使用 serviceId 配置的服务名作为请求前缀
* 默认服务路由规则的配置：zuul.routes.\<eureka-server-name\>=/\<eureka-server-name\>/**，其中 eureka-server-name 是注册服务的
服务名，如：
> http://localhost:5555/hello-service/hello1?accessToken=test-token&name=Tom
* 通过 zuul.ignored-services 参数设置一个服务名匹配表达式来定义不自动创建路由的规则
> 比如，设置为 zuul.ignored-services=* 的时候，Zuul 将对所有的服务都不自动创建路由规则。在这种情况下，就要在配置文件中逐个为需要路由的
服务添加映射规则了。只有在配置文件中出现的映射规则会被创建路由，而从 Eureka 中获取的其他服务，Zuul 将不会再为它们创建路由规则

# 自定义映射服务
> 详细见 SpringcloudZuulApplication 中实现的 PatternServiceRouteMapper

# 路径匹配
* 三种通配符
> 1.  ? 匹配任意单个字符，如：/user-service/a、/user-service/b
> 2.  * 匹配任意数量的字符，如：/user-service/a、/user-service/aaa，但无法匹配 /user-service/a/b
> 3.  ** 可以匹配 /user-service/* 包含的内容之外，还可以匹配如 /user-service/a/b 的多级目录路径
* 路由匹配规则是有序的
> 路由规则是通过 LinkedHashMap 保存的，也就是说，路由规则的保存是有序的，而内容的加载是通过遍历配置文件中的路由规则依次加入的。由于
properties 的配置内容无法保证有序，所以当为了保证路由的优先顺序时，需要使用 YALML 文件来配置，以实现有序的路由规则

# 忽略表达式
* 通过 zuul.ignored-patterns 设置不希望被 API 网关进行路由的 URL 表达式

# 路由前缀
* zuul.prefix 设置路由前缀
> 对于代理前缀会默认从路径中移除，如：/api/hello-service/hello，路由后：/hello-service/hello
* zuul.strip-prefix=false 关闭移除代理前缀的动作
* zuul.routes.\<route\>.strip-prefix=false 指定路由关闭移除代理前缀的动作

# 本地跳转
* 在路由功能中，Zuul 还支持 forward 形式的服务端跳转配置
> 如配置：zuul.routes.api-local.path=/api-local/** 和 zuul.routes.api-local.url=forward:/local，则当 API 网关接受到请求
/api-local/hello 时，该请求会被 API 网关转发到 网关 /local/hello 请求上进行本地处理。
> 由于进行了本地服务端跳转，在过滤器中设置 status 其实无效

# Cookie 与头信息
> 默认情况下，Spring Cloud Zuul 在请求路由时，会过滤掉 HTTP 请求头信息中的一些敏感信息，防止它们被传递到下游的外部服务器。默认的敏感信息
通过 zuul.sensitive-headers 参数定义，包括 Cookie、Set-Cookie、Authorization 三个属性。但如果我们使用 Spring Security、Shiro 等
安全框架构建的 Web 应用通过 Spring Cloud Zuul 构建的网关来进行路由时，由于 Cookie 信息无法传递，我们的 Web 应用将无法实现登陆和鉴权。
解决方法：
* 通过设置全局参数为空来覆盖默认值，zuul.sensitive-headers=
> 这种方法不推荐，破坏了默认设置的用意，在微服务架构中的 API 网关之内，对于无状态的 Restful API 请求肯定是要远远多于这些 Web 类应用请求。
* 通过指定路由的参数类配置，有两种方法：
> 1.  对指定路由开启自定义敏感头，zuul.routes.\<route\>customSensitiveHeaders=true
> 2.  指定路由的敏感头设置为空，zuul.routes.\<route\>.sensitiveHeaders=

# 重定向问题
> 虽然可以通过网关访问登陆也没并发起登录请求，但是登录成功之后，跳转到的也没 URL 却是具体 Web 应用实例的地址，而不是通过网关的路由地址。
这个问题非常严重，因为使用 API 网关的一个重要原因就是要将网关作为统一入口，从而不暴露所有的内部服务细节。  
> 引起问题的大致原因是由于 Spring Security 或 Shiro 在登录完成后，通过重定向的方式跳转到登录后的页面，此时登录后的请求结果状态吗为302，
请求响应信息中的 Location 指向了具体的服务实例地址，而请求头信息中的 Host 也指向了具体的服务实例 IP 地址和端口。所以，该问题的根本原因是
在于 Spring Cloud Zuul 在路由请求时，并没有将最初的 Host 信息设置正确，解决办法：
* zuul.add-host-header=true

# Hystrix 和 Ribbon 支持
> Zuul 包含了 spring-cloud-starter-hystrix 和 spring-cloud-starter-ribbon 模块的依赖，所以 Zuul 天生就拥有线程隔离和断路器的自我
保护功能，以及对服务调用的客户端负载均衡功能。注意，当使用 path 和 url 的映射关系（面向IP路由）来配置路由规则的时候，对于路由转发的请求不会
采用 HystrixCommand 来包装，所以这类路由请求没有线程隔离和断路器的保护，并且也不会有负载均衡的能力。因此，在使用 Zuul 时尽量使用 path 和
serviceId 的组合（面向服务路由）来进行配置，保证 API 网关的健壮和稳定，也能用到 Ribbon 的客户端负载均衡功能。   
> 在使用 Zuul 搭建 API 网关的时候，可以通过 Hystrix 和  Ribbon 的参数来调整路由请求的各种超时时间等配置，如：
* hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds：该参数用来设置 API 网关中路由转发请求的 HystrixCommand
执行的超时时间，单位为毫秒。当路由转发请求的命令执行时间超过配置值之后，Hystrix 会将该执行命令标记为 TIMEOUT 并抛出移除，Zuul 会对该异常
进行处理并返回 Json 信息给外部调用方
* ribbon.ConnectTimeout：该参数用来设置路由转发请求的时候，创建请求连接的超时时间。当 ribbon.ConnectTimeout 的配置值小于 
hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds 配置值的时候，若出现路由请求连接超时，会自动进行重试路由
请求，如果重试依然失败，Zuul 会返回如下 Json 信息给外部调用方；如果大于的时候，当出现路由请求超时时，直接按请求命令超时处理，返回 TIMEOUT
的错误信息
* ribbon.ReadTimeout：该参数用来设置路由转发请求的超时时间，与 ribbon.ConnectTimeout 类似，只是它的超时是对请求连接建立之后的处理时间
* zuul.retryable=false 或 zuul.routes.\<route\>.retryable=false 关闭重试机制
