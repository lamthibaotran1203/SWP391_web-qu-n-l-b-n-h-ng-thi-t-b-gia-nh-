package com.swp391.householdgoods.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SpaForwardControllerTest {

    @Test
    void forwardsKnownClientRoutesToTheReactEntryPoint() {
        SpaForwardController controller = new SpaForwardController();

        assertThat(controller.forwardKnownClientRoutes()).isEqualTo("forward:/index.html");
    }
}
