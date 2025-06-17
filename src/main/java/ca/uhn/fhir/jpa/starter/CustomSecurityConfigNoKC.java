package ca.uhn.fhir.jpa.starter;

import ca.uhn.fhir.jpa.starter.model.JWTPayload;
import ca.uhn.fhir.jpa.starter.service.FixNullReferenceInBundle;
import com.iprd.fhir.utils.Validation;
import interceptor.SignatureInterceptor;
import org.hibernate.annotations.common.util.impl.LoggerFactory;
import org.jboss.logging.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.OPTIONS;

//@ConditionalOnProperty(prefix = "keycloak", name = "enabled", havingValue = "true", matchIfMissing = true)
@Configuration
public class CustomSecurityConfigNoKC extends WebSecurityConfigurerAdapter {
	private static final String CORS_ALLOWED_HEADERS =
		"origin,content-type,accept,x-requested-with,Authorization,Access-Control-Allow-Credentials,kid";
	private static final String opensrpAllowedSources =
		"http://testhost.dashboard:3000/,http://localhost:3000/,https://oclink.io/,https://opencampaignlink.org/";
	private static final long corsMaxAge = 3600; // Updated to match DashboardController
	private static final Logger logger = LoggerFactory.logger(CustomSecurityConfigNoKC.class);

	@Autowired
	AppProperties appProperties;

	@Autowired
	private FixNullReferenceInBundle fixNullReferenceInBundle;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.addFilterBefore(new TokenAuthorizationFilter(), UsernamePasswordAuthenticationFilter.class)
			.addFilter(new SignatureInterceptor(appProperties, fixNullReferenceInBundle))
			.cors()
			.and()
			.authorizeRequests()
			.antMatchers("/iprd/web/**").authenticated()
			.anyRequest().permitAll()
			.and()
			.csrf().disable()
			.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);
	}

	@Bean
	public CorsConfigurationSource corsConfigurationSource() {
		CorsConfiguration configuration = new CorsConfiguration();
		configuration.setAllowedOrigins(Arrays.asList(opensrpAllowedSources.split(",")));
		configuration.setAllowedMethods(Arrays.asList(GET.name(), POST.name(), PUT.name(), DELETE.name(), OPTIONS.name()));
		configuration.setAllowedHeaders(Arrays.asList(CORS_ALLOWED_HEADERS.split(",")));
		configuration.setMaxAge(corsMaxAge);
		configuration.setAllowCredentials(true);
		UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", configuration);
		return source;
	}

	// Custom filter for token authorization
	public static class TokenAuthorizationFilter extends OncePerRequestFilter {
		private static final Logger logger = LoggerFactory.logger(TokenAuthorizationFilter.class);

		@Override
		protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
			String token = request.getHeader("Authorization");
			if (token == null || !token.startsWith("Bearer ")) {
				logger.warn("No valid Bearer token provided");
				response.setStatus(HttpStatus.FORBIDDEN.value());
				response.getWriter().write("Access denied: Missing or invalid Bearer token");
				return;
			}

			JWTPayload jwtPayload = Validation.getJWTToken(token.replace("Bearer ", ""));
			if (jwtPayload == null) {
				logger.warn("Invalid JWT token provided");
				response.setStatus(HttpStatus.FORBIDDEN.value());
				response.getWriter().write("Access denied: Invalid JWT token");
				return;
			}

			String userType = jwtPayload.getUser_type();
			String userName = jwtPayload.getName() != null ? jwtPayload.getName() : jwtPayload.getPreferred_username();
			if (!"web".equalsIgnoreCase(userType)) {
				logger.warn(String.format("Access denied for user: %s, user_type: %s",
					userName != null ? userName : "unknown",
					userType != null ? userType : "null"));
				response.setStatus(HttpStatus.FORBIDDEN.value());
				response.getWriter().write("Access denied for the User");
				return;
			}

			filterChain.doFilter(request, response);
		}
	}
}