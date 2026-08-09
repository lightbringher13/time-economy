package com.timeeconomy.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import com.timeeconomy.auth.domain.auth.model.AuthSession;
import com.timeeconomy.auth.domain.auth.port.in.RefreshUseCase;
import com.timeeconomy.auth.domain.auth.port.out.AuthSessionRepositoryPort;
import com.timeeconomy.auth.domain.auth.port.out.RefreshTokenPort;
import com.timeeconomy.auth.domain.exception.RefreshTokenReuseException;
import com.timeeconomy.auth.domain.outbox.model.OutboxEvent;
import com.timeeconomy.auth.domain.outbox.model.OutboxStatus;
import com.timeeconomy.auth.domain.outbox.port.out.OutboxEventRepositoryPort;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class AuthServiceApplicationTests {
	private static final long TEST_USER_ID = 987654321L;
	private static final String TEST_OUTBOX_AGGREGATE_TYPE = "outbox-recovery-test";

	@Container
	@ServiceConnection
	static final PostgreSQLContainer postgres = new PostgreSQLContainer(
			DockerImageName.parse("postgres:16-alpine")
	);

	@Autowired
	private RefreshUseCase refreshUseCase;

	@Autowired
	private AuthSessionRepositoryPort authSessionRepositoryPort;

	@Autowired
	private RefreshTokenPort refreshTokenPort;

	@Autowired
	private OutboxEventRepositoryPort outboxEventRepositoryPort;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private Clock clock;

	@AfterEach
	void cleanUpTestData() {
		jdbcTemplate.update("DELETE FROM auth_session WHERE user_id = ?", TEST_USER_ID);
		jdbcTemplate.update(
				"DELETE FROM outbox_events WHERE aggregate_type = ?",
				TEST_OUTBOX_AGGREGATE_TYPE
		);
	}

	@Test
	void contextLoads() {
	}

	@Test
	void reusedRefreshTokenCommitsReuseDetectionAndRevokesEntireFamily() {
		Instant now = Instant.now(clock);
		String familyId = UUID.randomUUID().toString();
		String reusedRawToken = refreshTokenPort.generateRefreshToken();

		AuthSession reusedSession = new AuthSession(
				TEST_USER_ID,
				familyId,
				refreshTokenPort.hashRefreshToken(reusedRawToken),
				"device-a",
				"198.51.100.10",
				"agent-a",
				now.minusSeconds(120),
				now.plusSeconds(3600)
		);
		reusedSession.revoke(now.minusSeconds(30));
		AuthSession savedReusedSession = authSessionRepositoryPort.save(reusedSession);

		AuthSession savedSuccessorSession = authSessionRepositoryPort.save(new AuthSession(
				TEST_USER_ID,
				familyId,
				refreshTokenPort.hashRefreshToken(refreshTokenPort.generateRefreshToken()),
				"device-a",
				"198.51.100.10",
				"agent-a",
				now.minusSeconds(20),
				now.plusSeconds(3600)
		));

		assertThrows(
				RefreshTokenReuseException.class,
				() -> refreshUseCase.refresh(new RefreshUseCase.RefreshCommand(
						reusedRawToken,
						"203.0.113.20",
						"different-agent",
						"different-device"
				))
		);

		Boolean reuseDetected = jdbcTemplate.queryForObject(
				"SELECT reuse_detected FROM auth_session WHERE id = ?",
				Boolean.class,
				savedReusedSession.getId()
		);
		Boolean successorRevoked = jdbcTemplate.queryForObject(
				"SELECT revoked FROM auth_session WHERE id = ?",
				Boolean.class,
				savedSuccessorSession.getId()
		);
		Long activeSessionCount = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM auth_session WHERE family_id = ? AND revoked = false",
				Long.class,
				familyId
		);

		assertEquals(Boolean.TRUE, reuseDetected);
		assertEquals(Boolean.TRUE, successorRevoked);
		assertEquals(0L, activeSessionCount);
	}

	@Test
	void processingOutboxEventIsReclaimedOnlyAfterLeaseExpires() {
		Instant firstClaimedAt = Instant.parse("2026-08-09T00:00:00Z");
		Duration lease = Duration.ofSeconds(10);

		OutboxEvent savedEvent = outboxEventRepositoryPort.save(OutboxEvent.newPending(
				TEST_OUTBOX_AGGREGATE_TYPE,
				UUID.randomUUID().toString(),
				"TestEvent.v1",
				"{}",
				firstClaimedAt
		));

		var firstClaim = outboxEventRepositoryPort.claimBatch(
				"dead-worker",
				1,
				lease,
				firstClaimedAt
		);

		assertEquals(1, firstClaim.size());
		assertEquals(savedEvent.getId(), firstClaim.getFirst().getId());
		assertEquals(OutboxStatus.PROCESSING, firstClaim.getFirst().getStatus());
		assertEquals("dead-worker", firstClaim.getFirst().getLockedBy());
		assertEquals(1, firstClaim.getFirst().getAttempts());

		var beforeLeaseExpiry = outboxEventRepositoryPort.claimBatch(
				"early-worker",
				1,
				lease,
				firstClaimedAt.plusSeconds(9)
		);

		assertTrue(beforeLeaseExpiry.isEmpty());

		var recoveredClaim = outboxEventRepositoryPort.claimBatch(
				"recovery-worker",
				1,
				lease,
				firstClaimedAt.plusSeconds(11)
		);

		assertEquals(1, recoveredClaim.size());
		assertEquals(savedEvent.getId(), recoveredClaim.getFirst().getId());
		assertEquals(OutboxStatus.PROCESSING, recoveredClaim.getFirst().getStatus());
		assertEquals("recovery-worker", recoveredClaim.getFirst().getLockedBy());
		assertEquals(2, recoveredClaim.getFirst().getAttempts());

		assertEquals(0, outboxEventRepositoryPort.markSent(
				savedEvent.getId(),
				"dead-worker",
				firstClaimedAt.plusSeconds(12),
				firstClaimedAt.plusSeconds(12)
		));
		assertEquals(1, outboxEventRepositoryPort.markSent(
				savedEvent.getId(),
				"recovery-worker",
				firstClaimedAt.plusSeconds(12),
				firstClaimedAt.plusSeconds(12)
		));

		String finalStatus = jdbcTemplate.queryForObject(
				"SELECT status FROM outbox_events WHERE id = ?",
				String.class,
				savedEvent.getId()
		);
		Integer attempts = jdbcTemplate.queryForObject(
				"SELECT attempts FROM outbox_events WHERE id = ?",
				Integer.class,
				savedEvent.getId()
		);

		assertEquals(OutboxStatus.SENT.name(), finalStatus);
		assertEquals(2, attempts);
	}

}
