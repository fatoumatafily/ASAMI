package com.asami.bot;

import com.asami.bot.config.OpenAiProperties;
import com.asami.bot.config.MetaSignupProperties;
import com.asami.bot.config.WhatsAppProperties;
import com.asami.bot.config.FrontendProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({
        WhatsAppProperties.class,
        OpenAiProperties.class,
        MetaSignupProperties.class,
        FrontendProperties.class
})
public class AsamiBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AsamiBackendApplication.class, args);
	}

}
