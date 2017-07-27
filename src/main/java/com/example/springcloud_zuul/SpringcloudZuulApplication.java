package com.example.springcloud_zuul;

import com.example.springcloud_zuul.filter.AccessFilter;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.filters.discovery.PatternServiceRouteMapper;
import org.springframework.context.annotation.Bean;

@EnableZuulProxy
@EnableDiscoveryClient
@SpringBootApplication
public class SpringcloudZuulApplication {

	@Bean
	public AccessFilter accessFilter() {
		return new AccessFilter();
	}

	/**
	 * 自定义服务与路由的映射 <br/>
	 * 如果服务命名为：userservice-v1、userservice-v2，则映射为：v1/userservice、v2/userservie
	 * @return
	 */
	@Bean
	public PatternServiceRouteMapper serviceRouteMapper() {
		// 第一个参数是用来匹配服务名称是否符合该自定义规则的正则表达式
		// 第二个参数是定义根据服务名中的定义内容转换处的路径表达式规则
		// 优先使用该实现构建处的路径表达式，如果没有匹配上的服务则还是使用默认的路由映射规则，即采用完整服务名作为前缀的路径表达式
		return new PatternServiceRouteMapper("(?<name>^.+)-(?<version>v.+$)",
				"${version}/${name}");
	}

	public static void main(String[] args) {
		SpringApplication.run(SpringcloudZuulApplication.class, args);
	}

}
