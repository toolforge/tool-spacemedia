package org.wikimedia.commons.donvip.spacemedia.apps;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

@ActiveProfiles({ "test", "job-eso" })
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(classes = SpacemediaOrgDjangoplicityUpdateJobApplication.class)
class SpacemediaOrgEsoUpdateJobApplicationTest {

    @Test
    void testContextLoads(ApplicationContext context) {
        assertNotNull(context);
    }
}
