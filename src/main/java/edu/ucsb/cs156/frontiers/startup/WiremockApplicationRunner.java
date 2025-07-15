package edu.ucsb.cs156.frontiers.startup;

import edu.ucsb.cs156.frontiers.services.wiremock.WiremockService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

@Configuration
@Slf4j
@Profile("wiremock")
public class WiremockApplicationRunner implements ApplicationRunner {
    @Autowired
    WiremockService wiremockService;

    /**
     * When using the wiremock profile, this method will call the code needed to set
     * up the wiremock services
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("wiremock mode");
        wiremockService.init();
        log.info("wiremockApplicationRunner completed");
    }

}
