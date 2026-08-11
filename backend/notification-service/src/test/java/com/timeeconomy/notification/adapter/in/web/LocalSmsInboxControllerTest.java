package com.timeeconomy.notification.adapter.in.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.URI;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.timeeconomy.notification.adapter.out.sms.InMemoryLocalSmsInboxAdapter;
import com.timeeconomy.notification.application.notification.port.out.LocalSmsInboxPort;
import com.timeeconomy.notification.application.notification.service.LocalSmsInboxService;
import com.timeeconomy.notification.config.LocalSmsInboxProperties;
import com.timeeconomy.notification.config.LocalSmsInboxTokenInterceptor;

class LocalSmsInboxControllerTest {

    private static final String TOKEN = "a-local-test-token-at-least-24-characters";
    private static final String HEADER = "X-Local-Sms-Inbox-Token";
    private static final String CANARY_PHONE = "+821012345678";
    private static final String CANARY_OTP = "731942";
    private static final Instant NOW = Instant.parse("2026-08-09T12:00:00Z");
    private static final UUID EVENT_ID = UUID.fromString("10000000-0000-0000-0000-000000000001");
    private static final UUID CHALLENGE_ID = UUID.fromString("20000000-0000-0000-0000-000000000002");

    private MockMvc mockMvc;
    private InMemoryLocalSmsInboxAdapter inbox;

    @BeforeEach
    void setUp() {
        LocalSmsInboxProperties properties = new LocalSmsInboxProperties(TOKEN, 100);
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        inbox = new InMemoryLocalSmsInboxAdapter(clock, properties);
        LocalSmsInboxController controller = new LocalSmsInboxController(
                new LocalSmsInboxService(inbox, clock)
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .addInterceptors(new LocalSmsInboxTokenInterceptor(properties))
                .build();
    }

    @Test
    void rejectsMissingOrInvalidToken() throws Exception {
        mockMvc.perform(get("/_local/sms-inbox"))
                .andExpect(status().isForbidden())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));

        mockMvc.perform(get("/_local/sms-inbox").header(HEADER, "wrong-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void listsOnlyMetadataAndConsumesOtpOnceWithoutCaching() throws Exception {
        inbox.store(entry());

        mockMvc.perform(get("/_local/sms-inbox").header(HEADER, TOKEN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(jsonPath("$[0].challengeId").value(CHALLENGE_ID.toString()))
                .andExpect(jsonPath("$[0].maskedDestination").value("********5678"))
                .andExpect(content().string(not(containsString(CANARY_PHONE))))
                .andExpect(content().string(not(containsString(CANARY_OTP))));

        mockMvc.perform(post("/_local/sms-inbox/{challengeId}/consume", CHALLENGE_ID)
                        .header(HEADER, TOKEN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"))
                .andExpect(jsonPath("$.challengeId").value(CHALLENGE_ID.toString()))
                .andExpect(jsonPath("$.otp").value(CANARY_OTP))
                .andExpect(content().string(not(containsString(CANARY_PHONE))));

        mockMvc.perform(post("/_local/sms-inbox/{challengeId}/consume", CHALLENGE_ID)
                        .header(HEADER, TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));
    }

    @Test
    void rejectsInvalidChallengeId() throws Exception {
        mockMvc.perform(post("/_local/sms-inbox/not-a-uuid/consume").header(HEADER, TOKEN))
                .andExpect(status().isBadRequest())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));
    }

    @Test
    void encodedPathsCannotBypassTokenProtection() throws Exception {
        inbox.store(entry());

        for (String path : new String[] {
                "/%5Flocal/sms-inbox",
                "/_local/%73ms-inbox",
                "/%5Flocal/%73ms-inbox"
        }) {
            mockMvc.perform(get(URI.create(path)))
                    .andExpect(status().is4xxClientError())
                    .andExpect(content().string(not(containsString(CANARY_OTP))));
        }
    }

    private LocalSmsInboxPort.Entry entry() {
        return new LocalSmsInboxPort.Entry(
                EVENT_ID,
                CHALLENGE_ID,
                CANARY_OTP,
                "********5678",
                "OTP_SMS",
                "SIGNUP_PHONE",
                NOW,
                NOW.plusSeconds(300)
        );
    }
}
