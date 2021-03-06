/**
 * Copyright 2011 The Buzz Media, LLC
 * Copyright 2015 Mickael Jeanroy <mickael.jeanroy@gmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thebuzzmedia.exiftool;

import com.thebuzzmedia.exiftool.core.schedulers.DefaultScheduler;
import com.thebuzzmedia.exiftool.core.schedulers.NoOpScheduler;
import com.thebuzzmedia.exiftool.core.strategies.DefaultStrategy;
import com.thebuzzmedia.exiftool.core.strategies.PoolStrategy;
import com.thebuzzmedia.exiftool.core.strategies.StayOpenStrategy;
import com.thebuzzmedia.exiftool.process.Command;
import com.thebuzzmedia.exiftool.process.CommandExecutor;
import com.thebuzzmedia.exiftool.process.CommandResult;
import com.thebuzzmedia.exiftool.process.executor.CommandExecutors;
import com.thebuzzmedia.exiftool.tests.builders.CommandResultBuilder;
import com.thebuzzmedia.exiftool.tests.builders.FileBuilder;
import com.thebuzzmedia.exiftool.tests.junit.SystemPropertyRule;
import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.thebuzzmedia.exiftool.tests.ReflectionUtils.readPrivateField;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
	CommandExecutors.class
})
public class ExifToolBuilderTest {

	@Rule
	public SystemPropertyRule exifToolProps = new SystemPropertyRule(
		"exiftool.path",
		"exiftool.processCleanupDelay"
	);

	@Mock
	private CommandExecutor executor;

	@Mock
	private ExecutionStrategy strategy;

	@Mock
	private Scheduler scheduler;

	private String path;

	private ExifToolBuilder builder;

	@Before
	public void setUp() throws Exception {
		initMocks(this);
		mockStatic(CommandExecutors.class);

		builder = new ExifToolBuilder();
		path = "/foo";

		// Mock ExifTool Version
		CommandResult v9_36 = new CommandResultBuilder()
			.output("9.36")
			.build();

		when(executor.execute(any(Command.class))).thenReturn(v9_36);
		when(strategy.isSupported(any(Version.class))).thenReturn(true);
	}

	@Test
	public void it_should_update_path() throws Exception {
		ExifToolBuilder res = builder.withPath(path);

		assertThat(res).isSameAs(builder);
		assertThat(readPrivateField(builder, "path", String.class))
			.isNotNull()
			.isEqualTo(path);
	}

	@Test
	public void it_should_update_path_with_file() throws Exception {
		File file = new FileBuilder("/usr/local/exiftool")
			.build();

		ExifToolBuilder res = builder.withPath(file);

		assertThat(res).isSameAs(builder);
		assertThat(readPrivateField(builder, "path", String.class))
			.isNotNull()
			.isEqualTo(file.getAbsolutePath());
	}

	@Test
	public void it_should_update_executor() throws Exception {
		ExifToolBuilder res = builder.withExecutor(executor);

		assertThat(res).isSameAs(builder);
		assertThat(readPrivateField(builder, "executor", CommandExecutor.class))
			.isNotNull()
			.isEqualTo(executor);
	}

	@Test
	public void it_should_enable_stay_open_feature() throws Exception {
		assertThat(readPrivateField(builder, "stayOpen", Boolean.class)).isNull();

		ExifToolBuilder r1 = builder.enableStayOpen();
		assertThat(r1).isSameAs(builder);
		assertThat(readPrivateField(builder, "stayOpen", Boolean.class)).isTrue();
		assertThat(readPrivateField(builder, "cleanupDelay", Long.class)).isNull();
	}

	@Test
	public void it_should_enable_stay_open_feature_with_delay() throws Exception {
		assertThat(readPrivateField(builder, "stayOpen", Boolean.class)).isNull();
		assertThat(readPrivateField(builder, "cleanupDelay", Boolean.class)).isNull();

		long delay = 10000;
		ExifToolBuilder r1 = builder.enableStayOpen(delay);

		assertThat(r1).isSameAs(builder);
		assertThat(readPrivateField(builder, "stayOpen", Boolean.class)).isTrue();
		assertThat(readPrivateField(builder, "cleanupDelay", Long.class)).isEqualTo(delay);
	}

	@Test
	public void it_should_enable_stay_open_feature_with_scheduler() throws Exception {
		assertThat(readPrivateField(builder, "stayOpen", Boolean.class)).isNull();
		assertThat(readPrivateField(builder, "scheduler", Scheduler.class)).isNull();

		ExifToolBuilder r1 = builder.enableStayOpen(scheduler);

		assertThat(r1).isSameAs(builder);
		assertThat(readPrivateField(builder, "stayOpen", Boolean.class)).isTrue();
		assertThat(readPrivateField(builder, "scheduler", Scheduler.class)).isSameAs(scheduler);
	}

	@Test
	public void it_should_override_strategy() throws Exception {
		assertThat(readPrivateField(builder, "strategy", ExecutionStrategy.class)).isNull();

		ExifToolBuilder r1 = builder.withStrategy(strategy);

		assertThat(r1).isSameAs(builder);
		assertThat(readPrivateField(builder, "strategy", ExecutionStrategy.class)).isSameAs(strategy);
	}

	@Test
	public void it_should_create_exiftool_with_custom_props() throws Exception {
		ExifTool exifTool = builder
			.withPath(path)
			.withExecutor(executor)
			.enableStayOpen()
			.build();

		assertThat(readPrivateField(exifTool, "path", String.class))
			.isNotNull()
			.isEqualTo(path);

		assertThat(readPrivateField(exifTool, "executor", CommandExecutor.class))
			.isNotNull()
			.isEqualTo(executor);

		ExecutionStrategy strategy = readPrivateField(exifTool, "strategy", ExecutionStrategy.class);
		assertThat(strategy)
			.isNotNull()
			.isExactlyInstanceOf(StayOpenStrategy.class);
	}

	@Test
	public void it_should_create_exiftool_and_get_path_from_system_property() throws Exception {
		System.setProperty("exiftool.path", path);

		ExifTool exifTool = builder
			.withExecutor(executor)
			.build();

		assertThat(readPrivateField(exifTool, "path", String.class))
			.isNotNull()
			.isEqualTo(path);
	}

	@Test
	public void it_should_create_exiftool_and_get_path_from_default() throws Exception {
		ExifTool exifTool = builder
			.withExecutor(executor)
			.build();

		assertThat(readPrivateField(exifTool, "path", String.class))
			.isNotNull()
			.isEqualTo("exiftool");
	}

	@Test
	public void it_should_create_exiftool_and_get_delay_from_default_property() throws Exception {
		long delay = 100;
		System.setProperty("exiftool.processCleanupDelay", String.valueOf(delay));

		ExifTool exifTool = builder
			.withExecutor(executor)
			.enableStayOpen()
			.build();

		ExecutionStrategy strategy = readPrivateField(exifTool, "strategy", ExecutionStrategy.class);
		assertThat(strategy)
			.isNotNull()
			.isExactlyInstanceOf(StayOpenStrategy.class);

		Scheduler scheduler = readPrivateField(strategy, "scheduler", Scheduler.class);
		assertThat(readPrivateField(scheduler, "delay", Long.class)).isEqualTo(delay);
		assertThat(readPrivateField(scheduler, "timeUnit", TimeUnit.class)).isEqualTo(TimeUnit.MILLISECONDS);
	}

	@Test
	public void it_should_create_exiftool_and_get_delay_from_default() throws Exception {
		ExifTool exifTool = builder
			.withExecutor(executor)
			.enableStayOpen()
			.build();

		ExecutionStrategy strategy = readPrivateField(exifTool, "strategy", ExecutionStrategy.class);
		assertThat(strategy)
			.isNotNull()
			.isExactlyInstanceOf(StayOpenStrategy.class);

		Scheduler scheduler = readPrivateField(strategy, "scheduler", Scheduler.class);
		assertThat(readPrivateField(scheduler, "delay", Long.class)).isEqualTo(600000);
		assertThat(readPrivateField(scheduler, "timeUnit", TimeUnit.class)).isEqualTo(TimeUnit.MILLISECONDS);
	}

	@Test
	public void it_should_create_exiftool_and_use_scheduler() throws Exception {
		ExifTool exifTool = builder
			.withExecutor(executor)
			.enableStayOpen(scheduler)
			.build();

		ExecutionStrategy strategy = readPrivateField(exifTool, "strategy", ExecutionStrategy.class);
		assertThat(strategy)
			.isNotNull()
			.isExactlyInstanceOf(StayOpenStrategy.class);

		assertThat(readPrivateField(strategy, "scheduler", Scheduler.class))
			.isNotNull()
			.isSameAs(scheduler);
	}

	@Test
	public void it_should_not_enable_stay_open_by_default() throws Exception {
		ExifTool exifTool = builder
			.withExecutor(executor)
			.build();

		ExecutionStrategy strategy = readPrivateField(exifTool, "strategy", ExecutionStrategy.class);
		assertThat(strategy)
			.isNotNull()
			.isExactlyInstanceOf(DefaultStrategy.class);
	}

	@Test
	public void it_should_create_with_default_executor() throws Exception {
		PowerMockito.when(CommandExecutors.newExecutor()).thenReturn(executor);

		ExifTool exifTool = builder
			.build();

		assertThat(readPrivateField(exifTool, "executor", CommandExecutor.class))
			.isNotNull()
			.isEqualTo(executor);
	}

	@Test
	public void it_should_create_with_custom_strategy() throws Exception {
		ExifTool exifTool = builder
			.withExecutor(executor)
			.withStrategy(strategy)
			.build();

		assertThat(readPrivateField(exifTool, "strategy", ExecutionStrategy.class))
			.isNotNull()
			.isEqualTo(strategy);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void it_should_create_with_pool_strategy() throws Exception {
		ExifTool exifTool = builder
			.withExecutor(executor)
			.withPoolSize(10, 500)
			.build();

		ExecutionStrategy strategy = readPrivateField(exifTool, "strategy", ExecutionStrategy.class);
		assertThat(strategy)
			.isNotNull()
			.isExactlyInstanceOf(PoolStrategy.class);

		BlockingQueue<ExecutionStrategy> pool = readPrivateField(strategy, "pool", BlockingQueue.class);
		assertThat(pool.size()).isEqualTo(10);
		assertThat(pool)
				.isNotEmpty()
				.are(new Condition<ExecutionStrategy>() {
					@Override
					public boolean matches(ExecutionStrategy value) {
						return value instanceof StayOpenStrategy;
					}
				})
				.are(new Condition<ExecutionStrategy>() {
					@Override
					public boolean matches(ExecutionStrategy value) {
						StayOpenStrategy stayOpenStrategy = (StayOpenStrategy) value;
						try {
							Scheduler scheduler = readPrivateField(stayOpenStrategy, "scheduler", Scheduler.class);
							return scheduler instanceof DefaultScheduler;
						}
						catch (Exception ex) {
							throw new AssertionError(ex);
						}
					}
				});
	}

	@Test
	public void it_should_not_create_pool_strategy_with_negative_pool() throws Exception {
		ExifTool exifTool = builder
				.withExecutor(executor)
				.withPoolSize(0, 0)
				.build();

		ExecutionStrategy strategy = readPrivateField(exifTool, "strategy", ExecutionStrategy.class);
		assertThat(strategy)
				.isNotNull()
				.isExactlyInstanceOf(DefaultStrategy.class);
	}

	@Test
	public void it_should_not_create_pool_strategy_with_negative_pool_without_delay() throws Exception {
		ExifTool exifTool = builder
				.withExecutor(executor)
				.withPoolSize(0)
				.build();

		ExecutionStrategy strategy = readPrivateField(exifTool, "strategy", ExecutionStrategy.class);
		assertThat(strategy)
				.isNotNull()
				.isExactlyInstanceOf(DefaultStrategy.class);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void it_should_create_with_pool_strategy_and_no_op_scheduler() throws Exception {
		ExifTool exifTool = builder
				.withExecutor(executor)
				.withPoolSize(10)
				.build();

		ExecutionStrategy strategy = readPrivateField(exifTool, "strategy", ExecutionStrategy.class);
		assertThat(strategy)
				.isNotNull()
				.isExactlyInstanceOf(PoolStrategy.class);

		BlockingQueue<ExecutionStrategy> pool = readPrivateField(strategy, "pool", BlockingQueue.class);
		assertThat(pool.size()).isEqualTo(10);
		assertThat(pool)
				.isNotEmpty()
				.are(new Condition<ExecutionStrategy>("Pool should only contains instance of StayOpenStrategy") {
					@Override
					public boolean matches(ExecutionStrategy value) {
						return value instanceof StayOpenStrategy;
					}
				})
				.are(new Condition<ExecutionStrategy>("Pool should only contains instance of NoOpScheduler.") {
					@Override
					public boolean matches(ExecutionStrategy value) {
						StayOpenStrategy stayOpenStrategy = (StayOpenStrategy) value;
						try {
							Scheduler scheduler = readPrivateField(stayOpenStrategy, "scheduler", Scheduler.class);
							return scheduler instanceof NoOpScheduler;
						}
						catch (Exception ex) {
							throw new AssertionError(ex);
						}
					}
				});
	}
}
