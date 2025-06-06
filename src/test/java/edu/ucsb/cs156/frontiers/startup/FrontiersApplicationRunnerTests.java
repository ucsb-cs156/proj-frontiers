package edu.ucsb.cs156.frontiers.startup;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.ApplicationArguments;

import static org.mockito.Mockito.*;

import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
public class FrontiersApplicationRunnerTests {

    @Autowired
    private FrontiersApplicationRunner runner;

    @MockBean
    private FrontiersStartup frontiersStartup;

    @Test
    public void test_run_calls_frontiersStartup() throws Exception {
        ApplicationArguments mockArgs = mock(ApplicationArguments.class);

        // Clear any invocations that happened during startup
        clearInvocations(frontiersStartup);

        runner.run(mockArgs);

        verify(frontiersStartup, times(1)).alwaysRunOnStartup();
    }
}
