package edu.ucsb.cs156.frontiers.startup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class FrontiersApplicationRunner implements ApplicationRunner {

    @Autowired
    private FrontiersStartup frontiersStartup;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        log.info("FrontiersApplicationRunner.run called");
        frontiersStartup.alwaysRunOnStartup();
    }
}
