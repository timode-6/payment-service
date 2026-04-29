package com.example.payment_service.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InternalAuthFilter")
class InternalAuthFilterTest {

    @InjectMocks
    private InternalAuthFilter filter;

    @Mock
    private FilterChain chain;

    private static final String VALID_SECRET = "test-secret";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(filter, "expectedSecret", VALID_SECRET);
        SecurityContextHolder.clearContext();
    }



    @Test
    void missingSecret_returns403() throws Exception {
        MockHttpServletRequest req  = new MockHttpServletRequest("GET", "/api/payments");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        assertThat(res.getContentAsString()).contains("Direct service access not allowed");
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void wrongSecret_returns403() throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/payments");
        req.addHeader(InternalAuthFilter.INTERNAL_SECRET_HEADER, "wrong-secret");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(HttpServletResponse.SC_FORBIDDEN);
        verify(chain, never()).doFilter(any(), any());
    }

    @Test
    void correctSecret_noUserHeaders_internalRole() throws Exception {
        MockHttpServletRequest req = request("GET", "/api/payments");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, chain);

        assertThat(res.getStatus()).isEqualTo(200);
        verify(chain).doFilter(req, res);
        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getPrincipal()).isEqualTo("internal-system");
        assertThat(auth.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_INTERNAL"));
    }

    @Test
    void userHeaders_setsUserPrincipal() throws Exception {
        MockHttpServletRequest req = request("GET", "/api/payments");
        req.addHeader(InternalAuthFilter.USER_ID_HEADER, "user-42");
        req.addHeader(InternalAuthFilter.USER_ROLE_HEADER, "USER");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getPrincipal()).isEqualTo("user-42");
        assertThat(auth.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
        verify(chain).doFilter(req, res);
    }

    @Test
    void adminHeaders_setsAdminPrincipal() throws Exception {
        MockHttpServletRequest req = request("GET", "/api/payments");
        req.addHeader(InternalAuthFilter.USER_ID_HEADER, "admin-1");
        req.addHeader(InternalAuthFilter.USER_ROLE_HEADER, "ADMIN");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getPrincipal()).isEqualTo("admin-1");
        assertThat(auth.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    @Test
    void userIdPresentRoleBlank_internalFallback() throws Exception {
        MockHttpServletRequest req = request("GET", "/api/payments");
        req.addHeader(InternalAuthFilter.USER_ID_HEADER, "user-99");
        req.addHeader(InternalAuthFilter.USER_ROLE_HEADER, "   ");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getAuthorities())
                .anyMatch(a -> a.getAuthority().equals("ROLE_INTERNAL"));
    }

    @Test
    void rolePresentUserIdBlank_internalFallback() throws Exception {
        MockHttpServletRequest req = request("GET", "/api/payments");
        req.addHeader(InternalAuthFilter.USER_ID_HEADER, "  ");
        req.addHeader(InternalAuthFilter.USER_ROLE_HEADER, "USER");
        MockHttpServletResponse res = new MockHttpServletResponse();

        filter.doFilterInternal(req, res, chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertThat(auth.getPrincipal()).isEqualTo("internal-system");
    }

    @Test
    void actuatorHealth_bypassed() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/actuator/health");
        assertThat(filter.shouldNotFilter(req)).isTrue();
    }

    @Test
    @DisplayName("/actuator/info → shouldNotFilter = true")
    void actuatorInfo_bypassed() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/actuator/info");
        assertThat(filter.shouldNotFilter(req)).isTrue();
    }

    @Test
    @DisplayName("/api/payments → shouldNotFilter = false")
    void paymentsPath_notBypassed() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/payments");
        assertThat(filter.shouldNotFilter(req)).isFalse();
    }

    @Test
    @DisplayName("/actuator/metrics → shouldNotFilter = false (not in public list)")
    void actuatorMetrics_notBypassed() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/actuator/metrics");
        assertThat(filter.shouldNotFilter(req)).isFalse();
    }

    private MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest req = new MockHttpServletRequest(method, uri);
        req.addHeader(InternalAuthFilter.INTERNAL_SECRET_HEADER, VALID_SECRET);
        return req;
    }
}