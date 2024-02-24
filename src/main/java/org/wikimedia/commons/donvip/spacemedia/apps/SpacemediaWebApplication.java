package org.wikimedia.commons.donvip.spacemedia.apps;

import static org.springframework.security.authorization.AuthenticatedAuthorizationManager.authenticated;
import static org.springframework.security.authorization.AuthorizationManagers.anyOf;
import static org.springframework.security.config.Customizer.withDefaults;
import static org.wikimedia.commons.donvip.spacemedia.utils.Utils.restTemplateSupportingAll;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.oauth2.client.http.OAuth2ErrorResponseErrorHandler;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.expression.WebExpressionAuthorizationManager;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import org.wikimedia.commons.donvip.spacemedia.data.domain.DomainDbConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.twelvemonkeys.servlet.image.IIOProviderContextListener;

@EnableAsync
@EnableCaching
@SpringBootConfiguration
@EnableAutoConfiguration
@Import(SpacemediaCommonConfiguration.class)
@ComponentScan(basePackages = { "org.wikimedia.commons.donvip.spacemedia.controller",
        "org.wikimedia.commons.donvip.spacemedia.data",
        "org.wikimedia.commons.donvip.spacemedia.service" }, excludeFilters = @ComponentScan.Filter(type = FilterType.REGEX, pattern = ".+Test.*"))
@EnableJpaRepositories(entityManagerFactoryRef = "domainEntityManagerFactory", transactionManagerRef = "domainTransactionManager", basePackageClasses = {
        DomainDbConfiguration.class })
public class SpacemediaWebApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpacemediaWebApplication.class, args);
    }

    @Bean
    public IIOProviderContextListener iioProviderContextListener() {
        return new IIOProviderContextListener();
    }

    @Bean
    @Profile("web")
    public SecurityFilterChain securityFilterChain(HttpSecurity http, ObjectMapper jackson,
            @Value("${rest.api.access.spring.security.expression}") String securityExpression) throws Exception {
        return http.authorizeHttpRequests(
                authz -> authz.requestMatchers("*/rest/**")
                        .access(anyOf(new WebExpressionAuthorizationManager(securityExpression), authenticated()))
                        .requestMatchers("/**").permitAll())
                .oauth2Login(
                        oauth2 -> oauth2
                                .userInfoEndpoint(userInfo -> userInfo.userService(this.oauth2UserService(jackson))))
                .oauth2Client(withDefaults()).build();
    }

    private OAuth2UserService<OAuth2UserRequest, OAuth2User> oauth2UserService(ObjectMapper jackson) {
        DefaultOAuth2UserService service = new DefaultOAuth2UserService();
        // Mediawiki return text/html instead of application/json
        RestTemplate restHtml = restTemplateSupportingAll(jackson);
        restHtml.setErrorHandler(new OAuth2ErrorResponseErrorHandler());
        service.setRestOperations(restHtml);
        return service;
    }

    @Bean
    public CommonsRequestLoggingFilter logFilter() {
        CommonsRequestLoggingFilter filter = new CommonsRequestLoggingFilter();
        filter.setIncludeQueryString(true);
        filter.setIncludePayload(true);
        filter.setMaxPayloadLength(10000);
        filter.setIncludeHeaders(true);
        filter.setIncludeClientInfo(true);
        filter.setAfterMessagePrefix("REQUEST DATA: ");
        return filter;
    }
}
