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

import com.timeeconomy.auth.adapter.in.internal.verification.dto.response.InternalVerificationOtpResponse;
import com.timeeconomy.auth.domain.verification.port.in.internal.AcknowledgeVerificationOtpDeliveryUseCase;
import com.timeeconomy.auth.domain.verification.port.in.internal.PeekVerificationOtpUseCase;

@ExtendWith(MockitoExtension.class)
class InternalVerificationOtpControllerTest {

    private static final String TOKEN = "internal-test-token-at-least-24-characters";
    private static final String TOKEN_HEADER = "X-Internal-Token";
    private static final String CHALLENGE_ID = "20000000-0000-0000-0000-000000000002";
    private static final String CANARY_OTP = "731942";
    private static final String PATH = "/internal/verification/challenges/{challengeId}/otp";

    @Mock
    private PeekVerificationOtpUseCase peekUseCase;
    @Mock
    private AcknowledgeVerificationOtpDeliveryUseCase acknowledgeUseCase;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        InternalVerificationOtpController controller = new InternalVerificationOtpController(
                peekUseCase,
                acknowledgeUseCase,
                TOKEN
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void peeksOtpWithTheCompatibleResponseShapeAndNoStoreHeaders() throws Exception {
        PeekVerificationOtpUseCase.Command command = new PeekVerificationOtpUseCase.Command(CHALLENGE_ID);
        when(peekUseCase.peek(command)).thenReturn(
                new PeekVerificationOtpUseCase.Result(CHALLENGE_ID, CANARY_OTP)
        );

        mockMvc.perform(get(PATH, CHALLENGE_ID).header(TOKEN_HEADER, TOKEN))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"))
                .andExpect(jsonPath("$.verificationChallengeId").value(CHALLENGE_ID))
                .andExpect(jsonPath("$.otp").value(CANARY_OTP));

        verify(peekUseCase).peek(command);
        verifyNoInteractions(acknowledgeUseCase);
    }

    @Test
    void returnsNotFoundWithoutCachingWhenOtpCannotBePeeked() throws Exception {
        PeekVerificationOtpUseCase.Command command = new PeekVerificationOtpUseCase.Command(CHALLENGE_ID);
        doThrow(new NoSuchElementException("otp not found")).when(peekUseCase).peek(command);

        mockMvc.perform(get(PATH, CHALLENGE_ID).header(TOKEN_HEADER, TOKEN))
                .andExpect(status().isNotFound())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));
    }

    @Test
    void rejectsMissingAndWrongTokensWithForbiddenForBothMethods() throws Exception {
        mockMvc.perform(get(PATH, CHALLENGE_ID))
                .andExpect(status().isForbidden())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));
        mockMvc.perform(get(PATH, CHALLENGE_ID).header(TOKEN_HEADER, "wrong-token"))
                .andExpect(status().isForbidden());
        mockMvc.perform(delete(PATH, CHALLENGE_ID))
                .andExpect(status().isForbidden())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")));
        mockMvc.perform(delete(PATH, CHALLENGE_ID).header(TOKEN_HEADER, "wrong-token"))
                .andExpect(status().isForbidden());

        verifyNoInteractions(peekUseCase, acknowledgeUseCase);
    }

    @Test
    void acknowledgesDeletionIdempotentlyWithNoContent() throws Exception {
        AcknowledgeVerificationOtpDeliveryUseCase.Command command =
                new AcknowledgeVerificationOtpDeliveryUseCase.Command(CHALLENGE_ID);

        mockMvc.perform(delete(PATH, CHALLENGE_ID).header(TOKEN_HEADER, TOKEN))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
                .andExpect(header().string(HttpHeaders.PRAGMA, "no-cache"));
        mockMvc.perform(delete(PATH, CHALLENGE_ID).header(TOKEN_HEADER, TOKEN))
                .andExpect(status().isNoContent());

        verify(acknowledgeUseCase, times(2)).acknowledge(command);
        verifyNoInteractions(peekUseCase);
    }

    @Test
    void rejectsBlankConfiguredInternalTokenAtConstruction() {
        assertThrows(
                IllegalStateException.class,
                () -> new InternalVerificationOtpController(peekUseCase, acknowledgeUseCase, " ")
        );
    }

    @Test
    void redactsRawOtpFromResponseToString() {
        InternalVerificationOtpResponse response = new InternalVerificationOtpResponse(CHALLENGE_ID, CANARY_OTP);

        assertFalse(response.toString().contains(CANARY_OTP));
    }
}
