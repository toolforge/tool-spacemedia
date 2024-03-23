package org.wikimedia.commons.donvip.spacemedia.apps;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles({ "test", "job-ussf-dvids" })
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = SpacemediaOrgDvidsUpdateJobApplication.class)
class SpacemediaOrgDvidsUpdateJobApplicationTest {

    @Test
    void testContextLoads(ApplicationContext context) {
        assertNotNull(context);
    }
}
