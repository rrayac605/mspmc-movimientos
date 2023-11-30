package mx.gob.imss.cit.pmc.mspmcmovimientos.security.model;

import java.io.Serializable;

import lombok.Data;

@Data
public class TokenInfoModel implements Serializable {
	private static final long serialVersionUID = 6383810427407944757L;
	private String issuer;
	private String audience; 
	private Long expires_in;
	private String token_id; 
	private String user_id;
	

}
