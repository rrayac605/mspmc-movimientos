package mx.gob.imss.cit.pmc.mspmcmovimientos.exception;

import mx.gob.imss.cit.mspmccommons.enums.EnumHttpStatus;
import mx.gob.imss.cit.pmc.mspmcmovimientos.model.RespuestaError;

public class BusinessException extends Exception {

	/**
	 *
	 */
	private static final long serialVersionUID = -872483897865904849L;

	private RespuestaError respuestaError;

	public BusinessException(EnumHttpStatus status,
			String businessMessage,
			String reasonPhrase,
			String cveBusinessMessage,
			String internalCode) {

		super(reasonPhrase);

		String completeBusinessMessage = businessMessage + " (" + internalCode + ")";

		respuestaError = new RespuestaError(status, completeBusinessMessage, reasonPhrase, cveBusinessMessage);

	}

	public BusinessException(RespuestaError respuestaError) {
		this.respuestaError = respuestaError;
	}

	public RespuestaError getRespuestaError() {
		return respuestaError;
	}

	public void setRespuestaError(RespuestaError respuestaError) {
		this.respuestaError = respuestaError;
	}

}
