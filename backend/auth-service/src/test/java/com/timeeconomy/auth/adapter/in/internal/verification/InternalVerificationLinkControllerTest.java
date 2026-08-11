package com.timeeconomy.auth.adapter.in.internal.verification;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.timeeconomy.auth.adapter.in.internal.verification.dto.response.InternalVerificationLinkResponse;
import com.timeeconomy.auth.domain.verification.model.VerificationPurpose;
import com.timeeconomy.auth.domain.verification.port.in.internal.AcknowledgeVerificationLinkDeliveryUseCase;
import com.timeeconomy.auth.domain.verification.port.in.internal.PeekVerificationLinkUrlUseCase;

@ExtendWith(MockitoExtension.class)
class InternalVerificationLinkControllerTest {

    private static final String TOKEN = "internal-test-token-at-least-24-characters";
    private static final String TOKEN_HEADER = "X-Internal-Token";
    private static final String CHALLENGE_ID = "30000000-0000-0000-0000-000000000003";
    private static final String CANARY_TOKEN = "raw-link-token-canary";
    private static final String CANARY_URL = "https://app.example.test/reset-password?token=" + CANARY_TOKEN;
    private static final String PATH = "/internal/verification/challenges/{challengeId}/link-url";

    @Mock
    private PeekVerificationLinkUrlUseCase peekUseCase;
    @Mock
    private AcknowledgeVerificationLinkDeliveryUseCase acknowledgeUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InternalVerificationLinkController controller = new InternalVerificationLinkController(
                peekUseCase,
                acknowledgeUseCase,
                TOKEN
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void peeksLinkUrlWithTheCompatibleResponseShapeAndNoStoreHeaders() throws Exception {
        PeekVerificationLinkUrlUseCase.Command command = new PeekVerificationLinkUrlUseCase.Command(
                CHALLENGE_ID,
                VerificationPurpose.PASSWORD_RESET
        );
        when(peekUseCase.peek(command)).thenReturn(
                new PeekVerificationLinkUrlUseCase.Result(CHALLENGE_ID, CANARY_URL)
        );

        mockMvc.perform(get(PATH, CHALLENGE_ID)
                        .param("purpose", VerificationPurpose.PASSWORD_RESET.name())
                        .header(TOKEN_HEADER, TOKEN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"))
                .andExpect(jsonPath("$.verificationChallengeId").value(CHALLENGE_ID))
                .andExpect(jsonPath("$.linkUrl").value(CANARY_URL));

        verify(peekUseCase).peek(command);
        verifyNoInteractions(acknowledgeUseCase);
    }

    @Test
    void returnsNotFoundWithoutCachingWhenLinkUrlCannotBePeeked() throws Exception {
        PeekVerificationLinkUrlUseCase.Command command = new PeekVerificationLinkUrlUseCase.Command(
                CHALLENGE_ID,
                VerificationPurpose.PASSWORD_RESET
        );
        doThrow(new NoSuchElementException("link URL not found")).when(peekUseCase).peek(command);

        mockMvc.perform(get(PATH, CHALLENGE_ID)
                        .param("purpose", VerificationPurpose.PASSWORD_RESET.name())
                        .header(TOKEN_HEADER, TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"));
    }

    @Test
    void rejectsMissingAndWrongTokensWithForbiddenForBothMethods() throws Exception {
        mockMvc.perform(get(PATH, CHALLENGE_ID)
                        .param("purpose", VerificationPurpose.PASSWORD_RESET.name()))
                .andExpect(status().isForbidden())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"));
        mockMvc.perform(get(PATH, CHALLENGE_ID)
                        .param("purpose", VerificationPurpose.PASSWORD_RESET.name())
                        .header(TOKEN_HEADER, "wrong-token"))
                .andExpect(status().isForbidden())
                .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"));
        mockMvc.perform(delete(PATH, CHALLENGE_ID))
                .andExpect(status().isForbidden())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"));
        mockMvc.perform(delete(PATH, CHALLENGE_ID).header(TOKEN_HEADER, "wrong-token"))
                .andExpect(status().isForbidden())
                .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"));

        verifyNoInteractions(peekUseCase, acknowledgeUseCase);
    }

    @Test
    void acknowledgesDeletionIdempotentlyWithNoContent() throws Exception {
        AcknowledgeVerificationLinkDeliveryUseCase.Command command =
                new AcknowledgeVerificationLinkDeliveryUseCase.Command(CHALLENGE_ID);

        mockMvc.perform(delete(PATH, CHALLENGE_ID).header(TOKEN_HEADER, TOKEN))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"));
        mockMvc.perform(delete(PATH, CHALLENGE_ID).header(TOKEN_HEADER, TOKEN))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"));

        verify(acknowledgeUseCase, times(2)).acknowledge(command);
        verifyNoInteractions(peekUseCase);
    }

    @Test
    void rejectsBlankConfiguredInternalTokenAtConstruction() {
        assertThrows(
                IllegalStateException.class,
                () -> new InternalVerificationLinkController(peekUseCase, acknowledgeUseCase, " ")
        );
    }

    @Test
    void redactsRawLinkUrlFromResponseToString() {
        InternalVerificationLinkResponse response = new InternalVerificationLinkResponse(
                CHALLENGE_ID,
                CANARY_URL
        );

        assertFalse(response.toString().contains(CANARY_URL));
        assertFalse(response.toString().contains(CANARY_TOKEN));
    }
}
