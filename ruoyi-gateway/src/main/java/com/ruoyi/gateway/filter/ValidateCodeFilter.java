package com.ruoyi.gateway.filter;

import java.nio.charset.StandardCharsets;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.core.io.buffer.NettyDataBufferFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.utils.ServletUtils;
import com.ruoyi.common.core.utils.StringUtils;
import com.ruoyi.gateway.config.properties.CaptchaProperties;
import com.ruoyi.gateway.service.ValidateCodeService;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * 验证码过滤器
 *
 * @author ruoyi
 */
@Component
public class ValidateCodeFilter extends AbstractGatewayFilterFactory<Object>
{
    private final static String[] VALIDATE_URL = new String[] { "/auth/login", "/auth/register" };

    @Autowired
    private ValidateCodeService validateCodeService;

    @Autowired
    private CaptchaProperties captchaProperties;

    private static final String CODE = "code";

    private static final String UUID = "uuid";

    @Override
    public GatewayFilter apply(Object config)
    {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // 非登录/注册请求或验证码关闭，不处理
            if (!StringUtils.equalsAnyIgnoreCase(request.getURI().getPath(), VALIDATE_URL) || !captchaProperties.getEnabled())
            {
                return chain.filter(exchange);
            }

            return DataBufferUtils.join(request.getBody()).flatMap(dataBuffer -> validate(exchange, chain, dataBuffer));
        };
    }

    private Mono<Void> validate(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain,
        DataBuffer dataBuffer)
    {
        byte[] content = new byte[dataBuffer.readableByteCount()];
        dataBuffer.read(content);
        DataBufferUtils.release(dataBuffer);
        String body = new String(content, StandardCharsets.UTF_8);

        try
        {
            JSONObject obj = JSON.parseObject(body);
            validateCodeService.checkCaptcha(obj.getString(CODE), obj.getString(UUID));
        }
        catch (Exception e)
        {
            return ServletUtils.webFluxResponseWriter(exchange.getResponse(), e.getMessage());
        }

        ServerHttpRequest decoratedRequest = new ServerHttpRequestDecorator(exchange.getRequest())
        {
            @Override
            public HttpHeaders getHeaders()
            {
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(super.getHeaders());
                headers.remove(HttpHeaders.CONTENT_LENGTH);
                headers.setContentLength(content.length);
                return headers;
            }

            @Override
            public Flux<DataBuffer> getBody()
            {
                NettyDataBufferFactory factory = new NettyDataBufferFactory(ByteBufAllocator.DEFAULT);
                return Flux.just(factory.wrap(content));
            }
        };

        return chain.filter(exchange.mutate().request(decoratedRequest).build());
    }
}
