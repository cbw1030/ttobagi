package com.ttobagi.web.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@Configuration
public class WebSecurityConfig extends WebSecurityConfigurerAdapter {

	@Autowired
	private DataSource dataSource;
	
	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http
			.authorizeRequests()
				//.antMatchers("/**").permitAll() // 편의를 위해 모든 페이지 접근 허용
				.antMatchers("/user/**").authenticated()
				.and()
			.formLogin()
				.loginPage("/auth/login")
				.loginProcessingUrl("/auth/login") // post하는 경로
				.defaultSuccessUrl("/index") // 의도적으로 로그인을 하여 성공한 경우
					.failureUrl("/auth/reg")
				.and()
			.logout()
				.logoutUrl("/auth/logout")
				.logoutSuccessUrl("/index")
				.invalidateHttpSession(true)
				.and()
			.csrf()
				.disable();
	}
	
	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth
			.jdbcAuthentication()
			.dataSource(dataSource)
			
			// 아이디에 따른 비밀번호 일치여부를 확인한다.
			// id, password, enabled를 뽑아야 스프링이 인식한다.
			.usersByUsernameQuery("select loginId id, password, 1 enabled "
					+ "from member where loginId=?")
			
			// 로그인에 성공한 사용자의 권한을 확인한다. 권한역할의 속성명은 반드시 'ROLE_역할' 형식으로 지정되어야 한다.
			// id, roleId를 뽑아야 스프링이 인식한다.
			.authoritiesByUsernameQuery("")
			.passwordEncoder(new BCryptPasswordEncoder());
	}
}