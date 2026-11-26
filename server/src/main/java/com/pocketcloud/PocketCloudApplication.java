package com.pocketcloud;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling            // runs the BuildWorker's @Scheduled poll
@ConfigurationPropertiesScan // binds records like BuildProperties
public class PocketCloudApplication {

	public static void main(String[] args) {
		SpringApplication.run(PocketCloudApplication.class, args);
	}

}
