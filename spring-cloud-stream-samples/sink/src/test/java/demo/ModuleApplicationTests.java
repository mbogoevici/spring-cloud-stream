package demo;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.hamcrest.core.IsEqual;
import org.hamcrest.core.IsNull;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.cloud.stream.adapter.ChannelBindingAdapter;
import org.springframework.cloud.stream.adapter.ChannelLocator;
import org.springframework.cloud.stream.adapter.DefaultChannelLocator;
import org.springframework.cloud.stream.adapter.InputChannelBinding;
import org.springframework.cloud.stream.annotation.ModuleChannels;
import org.springframework.cloud.stream.annotation.Output;
import org.springframework.cloud.stream.annotation.Sink;
import org.springframework.cloud.stream.annotation.Source;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import org.mockito.internal.matchers.NotNull;
import sink.LogSink;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SinkApplication.class)
@WebAppConfiguration
@DirtiesContext
public class ModuleApplicationTests {

	@Autowired
	@ModuleChannels(LogSink.class)
	private Sink sink;

	@Autowired
	private Sink same;


	@Autowired
	private ChannelBindingAdapter locator;


	// Not injected
	@Output(Source.OUTPUT)
	private MessageChannel output;

	@Test
	public void contextLoads() {
		assertNotNull(this.sink.input());
	}

	@Test
	public void bindings() {
		assertThat(locator.getInputChannel("input"), notNullValue(InputChannelBinding.class));
		assertThat(locator.getInputChannel("input").getRemoteName(), equalTo("testtock"));
	}

}
