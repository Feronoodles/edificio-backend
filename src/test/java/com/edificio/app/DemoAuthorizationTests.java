package com.edificio.app;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.beans.factory.annotation.Autowired;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "APP_DEMO_ENABLED=true",
        "APP_DEMO_USERNAME=demo",
        "APP_DEMO_PASSWORD=demo123",
        "APP_DEMO_EMAIL=demo@example.com"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
class DemoAuthorizationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void demoUserCanReadButCannotWrite() throws Exception {
        var login = mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "198.51.100.70")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "demo",
                                  "password": "demo123"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();
        var accessToken = JsonPath.<String>read(login, "$.accessToken");

        mockMvc.perform(get("/api/buildings")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/buildings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "No debe crearse",
                                  "address": "Av. Demo 123",
                                  "district": "Miraflores",
                                  "city": "Lima"
                                }
                                """))
                .andExpect(status().isForbidden());
    }
}
