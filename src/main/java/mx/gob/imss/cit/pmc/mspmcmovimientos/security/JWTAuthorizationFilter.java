package mx.gob.imss.cit.pmc.mspmcmovimientos.security;

import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import mx.gob.imss.cit.pmc.mspmcmovimientos.security.model.TokenInfoModel;
import mx.gob.imss.cit.pmc.mspmcmovimientos.security.service.TokenValidateService;


public class JWTAuthorizationFilter extends OncePerRequestFilter {
	
	protected final Logger log = Logger.getLogger(getClass().getName());
	
	private static final String HEADER = "Authorization";
	private static final String PREFIX = "Bearer ";
	private static final String ROL_SECURITY = "PROVEEDOR";
	
	protected TokenValidateService tokenValidateService;
	
	public JWTAuthorizationFilter(TokenValidateService tokenValidateService) {
		super();
		this.tokenValidateService = tokenValidateService;
	}


	@Override
	protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull  HttpServletResponse response,
									@NonNull FilterChain chain) throws ServletException, IOException {

		/*log.log(Level.INFO, "En el filtro");
			try {
				if (existeJWTToken(request)) {
					TokenInfoModel info = validateToken(request);
					if (info != null) {
						setUpSpringAuthentication(info);
					} else {
						SecurityContextHolder.clearContext();
						response.setStatus(HttpServletResponse.SC_FORBIDDEN);
						((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, "Sin acceso");
						return;
					}
				} else {
					SecurityContextHolder.clearContext();
					response.setStatus(HttpServletResponse.SC_FORBIDDEN);
					((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, "Sin acceso");
					return;
				}
			} catch (Exception e) {
				SecurityContextHolder.clearContext();
				response.setStatus(HttpServletResponse.SC_FORBIDDEN);
				response.setHeader("Message", e.getMessage());
				((HttpServletResponse) response).sendError(HttpServletResponse.SC_FORBIDDEN, e.getMessage());
				return;
			}*/
			chain.doFilter(request, response);
	}	

	private TokenInfoModel validateToken(HttpServletRequest request) {
		ResponseEntity<TokenInfoModel> response = tokenValidateService.validateToken(request);
		if(response.getStatusCodeValue()==(HttpServletResponse.SC_OK)){
			return response.getBody();
		}
		return null;
	}

	/**
	 * Metodo para autenticarnos dentro del flujo de Spring
	 * 
	 * @param tokenInfoModel
	 */
	private void setUpSpringAuthentication(TokenInfoModel tokenInfoModel) {

		String arrayAuthorities= ROL_SECURITY; 
	    String user = tokenInfoModel.getUser_id();
	    String password = tokenInfoModel.getToken_id();
	    
        List<GrantedAuthority> grantedAuths =
	                AuthorityUtils.commaSeparatedStringToAuthorityList(arrayAuthorities);
        UsernamePasswordAuthenticationToken auth =
	                new UsernamePasswordAuthenticationToken(user, password,grantedAuths);		
		SecurityContextHolder.getContext().setAuthentication(auth);

	}

	private boolean existeJWTToken(HttpServletRequest request) {
		String authenticationHeader = request.getHeader(HEADER);
		return !(authenticationHeader == null || !authenticationHeader.startsWith(PREFIX));
	}
	

}
