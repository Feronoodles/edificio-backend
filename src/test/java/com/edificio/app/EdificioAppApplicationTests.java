package com.edificio.app;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.blankOrNullString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
class EdificioAppApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void contextLoads() {
    }

    @Test
    void loginReturnsJwtAndProtectsBusinessEndpoints() throws Exception {
        mockMvc.perform(get("/api/buildings"))
                .andExpect(status().isUnauthorized());

        var login = login("198.51.100.10");
        var token = JsonPath.<String>read(login, "$.accessToken");
        var refreshToken = JsonPath.<String>read(login, "$.refreshToken");

        mockMvc.perform(get("/api/buildings")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/buildings")
                        .header("Authorization", "Bearer " + refreshToken))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken", not(blankOrNullString())))
                .andExpect(jsonPath("$.refreshToken", not(blankOrNullString())));
    }

    @Test
    void refreshTokenReuseRevokesActiveSessions() throws Exception {
        var login = login("198.51.100.20");
        var refreshToken = JsonPath.<String>read(login, "$.refreshToken");

        var refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        var rotatedRefreshToken = JsonPath.<String>read(refreshResult, "$.refreshToken");

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Refresh token reutilizado. Todas las sesiones fueron revocadas."));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "%s"
                                }
                                """.formatted(rotatedRefreshToken)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void loginRateLimitRejectsTooManyAttemptsFromSameClient() throws Exception {
        for (int i = 0; i < 100; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .header("X-Forwarded-For", "198.51.100.30")
                            .contentType(APPLICATION_JSON)
                            .content("""
                                    {
                                      "username": "admin",
                                      "password": "wrong"
                                    }
                                    """))
                    .andExpect(status().isUnauthorized());
        }

        mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", "198.51.100.30")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "wrong"
                                }
                                """))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Demasiados intentos de login. Intenta nuevamente mas tarde."));
    }

    @Test
    void auditFieldsAreReturnedForApartmentsResidentsAndPayments() throws Exception {
        var accessToken = JsonPath.<String>read(login("198.51.100.40"), "$.accessToken");

        var buildingId = JsonPath.<Integer>read(mockMvc.perform(post("/api/buildings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Torre Auditoria",
                                  "address": "Av. Test 123",
                                  "district": "Miraflores",
                                  "city": "Lima"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(), "$.id");

        var apartmentResponse = mockMvc.perform(post("/api/apartments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "buildingId": %d,
                                  "number": "A-901",
                                  "floor": 9,
                                  "areaM2": 82.5,
                                  "occupied": false
                                }
                                """.formatted(buildingId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value("admin"))
                .andExpect(jsonPath("$.createdAt", not(blankOrNullString())))
                .andReturn()
                .getResponse()
                .getContentAsString();
        var apartmentId = JsonPath.<Integer>read(apartmentResponse, "$.id");

        mockMvc.perform(put("/api/apartments/{id}", apartmentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "buildingId": %d,
                                  "number": "A-901",
                                  "floor": 9,
                                  "areaM2": 84.0,
                                  "occupied": true
                                }
                                """.formatted(buildingId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedBy").value("admin"))
                .andExpect(jsonPath("$.updatedAt", not(blankOrNullString())));

        mockMvc.perform(post("/api/residents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "apartmentId": %d,
                                  "firstName": "Ana",
                                  "lastName": "Auditoria",
                                  "documentNumber": "DOC-901",
                                  "email": "ana.auditoria@example.com",
                                  "phone": "999999999",
                                  "owner": true,
                                  "active": true
                                }
                                """.formatted(apartmentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value("admin"))
                .andExpect(jsonPath("$.createdAt", not(blankOrNullString())));

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "apartmentId": %d,
                                  "concept": "Mantenimiento auditoria",
                                  "amount": 150.00,
                                  "dueDate": "2026-06-30",
                                  "paidAt": null,
                                  "status": "PENDING"
                                }
                                """.formatted(apartmentId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.createdBy").value("admin"))
                .andExpect(jsonPath("$.createdAt", not(blankOrNullString())));
    }

    @Test
    void deleteBuildingAndApartmentExplainDependencyConflicts() throws Exception {
        var accessToken = JsonPath.<String>read(login("198.51.100.50"), "$.accessToken");

        var buildingId = JsonPath.<Integer>read(mockMvc.perform(post("/api/buildings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Torre Dependencias",
                                  "address": "Av. Dependencia 456",
                                  "district": "San Isidro",
                                  "city": "Lima"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(), "$.id");

        var apartmentId = JsonPath.<Integer>read(mockMvc.perform(post("/api/apartments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "buildingId": %d,
                                  "number": "D-101",
                                  "floor": 1,
                                  "areaM2": 60.0,
                                  "occupied": true
                                }
                                """.formatted(buildingId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(), "$.id");

        mockMvc.perform(delete("/api/buildings/{id}", buildingId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "No se puede eliminar el edificio porque tiene 1 departamento(s) activo(s). Elimina primero sus residentes, pagos y departamentos."
                ));

        mockMvc.perform(post("/api/residents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "apartmentId": %d,
                                  "firstName": "Luis",
                                  "lastName": "Dependencia",
                                  "documentNumber": "DOC-DEP",
                                  "email": "luis.dependencia@example.com",
                                  "phone": "988888888",
                                  "owner": false,
                                  "active": true
                                }
                                """.formatted(apartmentId)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/payments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "apartmentId": %d,
                                  "concept": "Mantenimiento dependencia",
                                  "amount": 90.00,
                                  "dueDate": "2026-06-30",
                                  "paidAt": null,
                                  "status": "PENDING"
                                }
                                """.formatted(apartmentId)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/apartments/{id}", apartmentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "No se puede eliminar el departamento porque tiene 1 residente(s) y 1 pago(s) activo(s). Elimina primero esos registros."
                ));
    }

    @Test
    void softDeletedRecordsDoNotBlockRecreatingCommonBusinessValues() throws Exception {
        var accessToken = JsonPath.<String>read(login("198.51.100.60"), "$.accessToken");

        var buildingId = JsonPath.<Integer>read(mockMvc.perform(post("/api/buildings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "San Soto",
                                  "address": "Los Laureles 365",
                                  "district": "Surco",
                                  "city": "Lima"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(), "$.id");

        var apartmentId = JsonPath.<Integer>read(mockMvc.perform(post("/api/apartments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "buildingId": %d,
                                  "number": "101",
                                  "floor": 1,
                                  "areaM2": 50.0,
                                  "occupied": false
                                }
                                """.formatted(buildingId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(), "$.id");

        mockMvc.perform(delete("/api/apartments/{id}", apartmentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/apartments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "buildingId": %d,
                                  "number": "101",
                                  "floor": 1,
                                  "areaM2": 52.0,
                                  "occupied": false
                                }
                                """.formatted(buildingId)))
                .andExpect(status().isCreated());

        mockMvc.perform(delete("/api/buildings/{id}", buildingId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict());

        mockMvc.perform(post("/api/buildings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "San Soto",
                                  "address": "Los Laureles 999",
                                  "district": "Surco",
                                  "city": "Lima"
                                }
                                """))
                .andExpect(status().isCreated());

        var archivedOnlyBuildingId = JsonPath.<Integer>read(mockMvc.perform(post("/api/buildings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Torre Archivada",
                                  "address": "Calle Archivo 10",
                                  "district": "Surco",
                                  "city": "Lima"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(), "$.id");

        var archivedApartmentId = JsonPath.<Integer>read(mockMvc.perform(post("/api/apartments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "buildingId": %d,
                                  "number": "301",
                                  "floor": 3,
                                  "areaM2": 64.0,
                                  "occupied": false
                                }
                                """.formatted(archivedOnlyBuildingId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(), "$.id");

        mockMvc.perform(delete("/api/apartments/{id}", archivedApartmentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/buildings/{id}", archivedOnlyBuildingId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        var cleanBuildingId = JsonPath.<Integer>read(mockMvc.perform(post("/api/buildings")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Torre Residentes",
                                  "address": "Av. Personas 100",
                                  "district": "Surco",
                                  "city": "Lima"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(), "$.id");

        var cleanApartmentId = JsonPath.<Integer>read(mockMvc.perform(post("/api/apartments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "buildingId": %d,
                                  "number": "202",
                                  "floor": 2,
                                  "areaM2": 70.0,
                                  "occupied": true
                                }
                                """.formatted(cleanBuildingId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(), "$.id");

        var residentId = JsonPath.<Integer>read(mockMvc.perform(post("/api/residents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "apartmentId": %d,
                                  "firstName": "Maria",
                                  "lastName": "Recrear",
                                  "documentNumber": "DOC-RECREATE",
                                  "email": "maria.recrear@example.com",
                                  "phone": "977777777",
                                  "owner": true,
                                  "active": true
                                }
                                """.formatted(cleanApartmentId)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString(), "$.id");

        mockMvc.perform(delete("/api/residents/{id}", residentId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/residents")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "apartmentId": %d,
                                  "firstName": "Maria",
                                  "lastName": "Nueva",
                                  "documentNumber": "DOC-RECREATE",
                                  "email": "maria.nueva@example.com",
                                  "phone": "966666666",
                                  "owner": false,
                                  "active": true
                                }
                                """.formatted(cleanApartmentId)))
                .andExpect(status().isCreated());
    }

    private String login(String forwardedFor) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .header("X-Forwarded-For", forwardedFor)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "admin",
                                  "password": "admin123"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken", not(blankOrNullString())))
                .andExpect(jsonPath("$.refreshToken", not(blankOrNullString())))
                .andReturn()
                .getResponse()
                .getContentAsString();
    }
}
