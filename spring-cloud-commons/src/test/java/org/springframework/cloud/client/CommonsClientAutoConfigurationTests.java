/*
 * Copyright 2012-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.cloud.client;

import org.junit.Test;

import org.springframework.beans.BeansException;
import org.springframework.boot.actuate.autoconfigure.health.HealthEndpointAutoConfiguration;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.client.actuator.FeaturesEndpoint;
import org.springframework.cloud.client.actuator.HasFeatures;
import org.springframework.cloud.client.discovery.health.DiscoveryClientHealthIndicator;
import org.springframework.cloud.client.discovery.health.DiscoveryCompositeHealthIndicator;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.assertj.core.api.BDDAssertions.then;

/**
 * @author Spencer Gibb
 * @author Olga Maciaszek-Sharma
 * @author Tim Ysewyn
 */
public class CommonsClientAutoConfigurationTests {

	ApplicationContextRunner applicationContextRunner = new ApplicationContextRunner()
			.withConfiguration(
					AutoConfigurations.of(HealthEndpointAutoConfiguration.class,
							CommonsClientAutoConfiguration.class,
							SimpleDiscoveryClientAutoConfiguration.class,
							UtilAutoConfiguration.class));

	@Test
	public void beansCreatedNormally() {
		applicationContextRunner.run(ctxt -> {
			then(ctxt.getBean(DiscoveryClientHealthIndicator.class)).isNotNull();
			then(ctxt.getBean(DiscoveryCompositeHealthIndicator.class)).isNotNull();
			then(ctxt.getBean(FeaturesEndpoint.class)).isNotNull();
			then(ctxt.getBeansOfType(HasFeatures.class).values()).isNotEmpty();
		});
	}

	@Test
	public void disableAll() {
		applicationContextRunner
				.withPropertyValues("spring.cloud.discovery.enabled=false").run(ctxt -> {
					assertBeanNonExistant(ctxt, DiscoveryClientHealthIndicator.class);
					assertBeanNonExistant(ctxt, DiscoveryCompositeHealthIndicator.class);
					then(ctxt.getBean(FeaturesEndpoint.class)).isNotNull();
					// features actuator is independent of discovery
					assertBeanNonExistant(ctxt, HasFeatures.class);
				});
	}

	@Test
	public void disableBlocking() {
		applicationContextRunner
				.withPropertyValues("spring.cloud.discovery.blocking.enabled=false")
				.run(ctxt -> {
					assertBeanNonExistant(ctxt, DiscoveryClientHealthIndicator.class);
					assertBeanNonExistant(ctxt, DiscoveryCompositeHealthIndicator.class);
					then(ctxt.getBean(FeaturesEndpoint.class)).isNotNull();
					// features actuator is independent of discovery
					assertBeanNonExistant(ctxt, HasFeatures.class);
				});
	}

	@Test
	public void disableAllIndividually() {
		applicationContextRunner.withPropertyValues(
				"spring.cloud.discovery.client.health-indicator.enabled=false",
				"spring.cloud.discovery.client.composite-indicator.enabled=false",
				"spring.cloud.features.enabled=false").run(ctxt -> {
					assertBeanNonExistant(ctxt, DiscoveryClientHealthIndicator.class);
					assertBeanNonExistant(ctxt, DiscoveryCompositeHealthIndicator.class);
					assertBeanNonExistant(ctxt, FeaturesEndpoint.class);
				});
	}

	@Test
	public void disableHealthIndicator() {
		applicationContextRunner
				.withPropertyValues(
						"spring.cloud.discovery.client.health-indicator.enabled=false")
				.run(ctxt -> {
					assertBeanNonExistant(ctxt, DiscoveryClientHealthIndicator.class);
					assertBeanNonExistant(ctxt, DiscoveryCompositeHealthIndicator.class);
				});
	}

	@Test
	public void conditionalOnDiscoveryEnabledWorks() {
		applicationContextRunner.withUserConfiguration(DiscoveryEnabledConfig.class)
				.withPropertyValues("spring.cloud.discovery.enabled=false")
				.run(context -> assertBeanNonExistant(context, TestBean.class));
		applicationContextRunner.withUserConfiguration(DiscoveryEnabledConfig.class)
				.withPropertyValues("spring.cloud.discovery.enabled=true")
				.run(context -> assertThat(context.getBean(TestBean.class)).isNotNull());
	}

	@Test
	public void conditionalOnBlockingDiscoveryEnabledWorks() {
		applicationContextRunner
				.withUserConfiguration(BlockingDiscoveryEnabledConfig.class)
				.withPropertyValues("spring.cloud.discovery.blocking.enabled=false")
				.run(context -> assertBeanNonExistant(context, TestBean.class));
		applicationContextRunner
				.withUserConfiguration(BlockingDiscoveryEnabledConfig.class)
				.withPropertyValues("spring.cloud.discovery.blocking.enabled=true")
				.run(context -> assertThat(context.getBean(TestBean.class)).isNotNull());
	}

	private void assertBeanNonExistant(ConfigurableApplicationContext ctxt,
			Class<?> beanClass) {
		try {
			ctxt.getBean(beanClass);
			fail("Bean of type " + beanClass + " should not have been created");
		}
		catch (BeansException e) {
			// should fail with exception
		}
	}

	@Configuration
	@ConditionalOnDiscoveryEnabled
	protected static class DiscoveryEnabledConfig {

		@Bean
		TestBean testBean() {
			return new TestBean();
		}

	}

	@TestConfiguration
	@ConditionalOnBlockingDiscoveryEnabled
	protected static class BlockingDiscoveryEnabledConfig {

		@Bean
		TestBean testBean() {
			return new TestBean();
		}

	}

	private static class TestBean {

	}

}