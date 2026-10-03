package com.spendwise.service;

import com.spendwise.config.SeedDataConfig;
import com.spendwise.config.properties.TelegramProperties;
import com.spendwise.entity.TelegramAccount;
import com.spendwise.entity.TelegramInvite;
import com.spendwise.entity.UserProfile;
import com.spendwise.repository.TelegramAccountRepository;
import com.spendwise.repository.TelegramInviteRepository;
import com.spendwise.repository.UserProfileRepository;
import com.spendwise.model.TelegramAccountStatus;
import com.spendwise.model.TelegramInviteStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TelegramAuthorizationServiceTest {
    @Mock TelegramAccountRepository accounts;
    @Mock TelegramInviteRepository invites;
    @Mock UserProfileRepository users;
    @Mock TelegramProperties properties;
    @Mock SeedDataConfig seedData;

    private TelegramAuthorizationService service() {
        return new TelegramAuthorizationService(accounts, invites, users, properties, seedData);
    }

    @Test
    void inviteIsGenericAndPersistsOnlyTokenHash() {
        when(properties.getBotUsername()).thenReturn("SpendWiseBot");
        when(properties.getInviteExpirationMinutes()).thenReturn(30);
        when(invites.save(any(TelegramInvite.class))).thenAnswer(call -> call.getArgument(0));

        var created = service().createInvite();

        String rawToken = created.inviteUrl().substring(created.inviteUrl().indexOf("?start=") + 7);
        ArgumentCaptor<TelegramInvite> captor = ArgumentCaptor.forClass(TelegramInvite.class);
        verify(invites).save(captor.capture());
        assertTrue(created.inviteUrl().startsWith("https://t.me/SpendWiseBot?start="));
        assertEquals(43, rawToken.length());
        assertNotEquals(rawToken, captor.getValue().getTokenHash());
        assertNull(captor.getValue().getUser());
        assertEquals(TelegramInviteStatus.ACTIVE, captor.getValue().getStatus());
    }

    @Test
    void validInviteBecomesPendingClaimWithoutCreatingUserOrAccount() {
        String token = "a-long-invitation-token";
        TelegramInvite invite = activeInvite(token);
        when(invites.findByTokenHashForUpdate(TelegramAuthorizationService.sha256(token))).thenReturn(Optional.of(invite));
        when(accounts.findByTelegramUserIdForUpdate("12345")).thenReturn(Optional.empty());
        when(users.findByTelegramId("12345")).thenReturn(Optional.empty());

        var result = service().claim("12345", token, "dipesh", "D", "K");

        assertEquals(TelegramAuthorizationService.ClaimStatus.PENDING_APPROVAL, result.status());
        assertEquals(TelegramInviteStatus.CLAIMED, invite.getStatus());
        assertEquals("12345", invite.getUsedByTelegramUserId());
        assertEquals("dipesh", invite.getTelegramUsername());
        assertNotNull(invite.getClaimedAt());
        verify(accounts, never()).save(any());
        verify(users, never()).save(any());
    }

    @Test
    void sameClaimantCanRetryPendingInviteButDifferentClaimantCannot() {
        String token = "token";
        TelegramInvite invite = activeInvite(token);
        invite.setStatus(TelegramInviteStatus.CLAIMED);
        invite.setUsedByTelegramUserId("12345");
        when(invites.findByTokenHashForUpdate(TelegramAuthorizationService.sha256(token))).thenReturn(Optional.of(invite));

        assertEquals(TelegramAuthorizationService.ClaimStatus.PENDING_APPROVAL,
                service().claim("12345", token, null, null, null).status());
        assertEquals(TelegramAuthorizationService.ClaimStatus.INVITE_ALREADY_USED,
                service().claim("67890", token, null, null, null).status());
    }

    @Test
    void adminApprovalCreatesSpendwiseUserAndActiveTelegramMapping() {
        TelegramInvite invite = activeInvite("token");
        invite.setStatus(TelegramInviteStatus.CLAIMED);
        invite.setUsedByTelegramUserId("12345");
        invite.setTelegramFirstName("D");
        invite.setTelegramLastName("K");
        when(invites.findByIdForUpdate(any())).thenReturn(Optional.of(invite));
        when(accounts.findByTelegramUserIdForUpdate("12345")).thenReturn(Optional.empty());
        when(users.findByTelegramId("12345")).thenReturn(Optional.empty());
        when(users.save(any(UserProfile.class))).thenAnswer(call -> call.getArgument(0));
        when(accounts.save(any(TelegramAccount.class))).thenAnswer(call -> call.getArgument(0));

        service().approveClaim(java.util.UUID.randomUUID());

        ArgumentCaptor<UserProfile> userCaptor = ArgumentCaptor.forClass(UserProfile.class);
        verify(users).save(userCaptor.capture());
        assertEquals("12345", userCaptor.getValue().getTelegramId());
        assertEquals("D K", userCaptor.getValue().getDisplayName());
        assertEquals("INR", userCaptor.getValue().getBaseCurrency());
        verify(seedData).seedDefaultCategories(userCaptor.getValue());
        ArgumentCaptor<TelegramAccount> accountCaptor = ArgumentCaptor.forClass(TelegramAccount.class);
        verify(accounts).save(accountCaptor.capture());
        assertEquals(TelegramAccountStatus.ACTIVE, accountCaptor.getValue().getStatus());
        assertSame(userCaptor.getValue(), accountCaptor.getValue().getUser());
        assertEquals(TelegramInviteStatus.USED, invite.getStatus());
        assertNotNull(invite.getUsedAt());
    }

    @Test
    void pendingClaimIsReportedAsPendingNotAuthorized() {
        when(accounts.findByTelegramUserId("12345")).thenReturn(Optional.empty());
        when(invites.findFirstByUsedByTelegramUserIdAndStatus("12345", TelegramInviteStatus.CLAIMED)).thenReturn(Optional.of(new TelegramInvite()));
        assertEquals("PENDING_APPROVAL", service().getAuthorization("12345").status());
    }

    @Test
    void expiredInviteCannotBeClaimed() {
        TelegramInvite invite = activeInvite("expired");
        invite.setExpiresAt(Instant.now().minusSeconds(1));
        when(invites.findByTokenHashForUpdate(TelegramAuthorizationService.sha256("expired"))).thenReturn(Optional.of(invite));
        assertEquals(TelegramAuthorizationService.ClaimStatus.INVITE_EXPIRED,
                service().claim("12345", "expired", null, null, null).status());
        assertEquals(TelegramInviteStatus.EXPIRED, invite.getStatus());
    }

    private TelegramInvite activeInvite(String token) {
        TelegramInvite invite = new TelegramInvite();
        invite.setTokenHash(TelegramAuthorizationService.sha256(token));
        invite.setStatus(TelegramInviteStatus.ACTIVE);
        invite.setExpiresAt(Instant.now().plusSeconds(60));
        return invite;
    }
}
