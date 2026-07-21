package com.example.demo.support

import org.springframework.beans.factory.config.BeanPostProcessor
import org.springframework.boot.web.servlet.ServletRegistrationBean
import org.springframework.stereotype.Component

/**
 * H2 콘솔이 접속 정보를 홈(~/.h2.server.properties)이 아니라 저장소 루트에서 읽게 한다.
 * 덕분에 .h2.server.properties 를 커밋해두면 누가 클론해도 콘솔에서 Connect 만 누르면 된다.
 *
 * H2 서블릿은 초기화 파라미터를 "-이름 값" 인자로 바꿔 WebServer.init() 에 넘기고,
 * 거기서 -properties 가 설정 디렉터리(serverPropertiesDir)가 된다.
 * Boot 의 h2Console 빈에는 @ConditionalOnMissingBean 이 없어 재정의가 안 되므로,
 * 이미 만들어진 등록 빈에 파라미터만 얹는다.
 */
@Component
class H2ConsoleSettings : BeanPostProcessor {

    override fun postProcessBeforeInitialization(bean: Any, beanName: String): Any {
        if (beanName == "h2Console" && bean is ServletRegistrationBean<*>) {
            bean.addInitParameter("properties", ".")
        }
        return bean
    }
}
