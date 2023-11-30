package mx.gob.imss.cit.pmc.mspmcmovimientos.model;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.validation.constraints.NotNull;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import lombok.Data;
import mx.gob.imss.cit.mspmccommons.enums.EnumHttpStatus;


@Data
@ApiModel(description = "Tipo de Dato para transportar un posible error en la aplicación")
public class RespuestaError implements Serializable {

	private static final long serialVersionUID = 1L;

	@ApiModelProperty(required = true, value = "Código de error")
    @NotNull
	private String code;
	@ApiModelProperty(required = true, value = "Descripción del error")
    @NotNull	
	private String description;
	@ApiModelProperty(required = true, value = "Mensaje del BusinessException")
    @NotNull
	private String businessMessage;
	@ApiModelProperty(required = true, value = "Frase de la razón del error")
    @NotNull
	private String reasonPhrase;
	@ApiModelProperty(required = true, value = "URI donde se originó el error")
    @NotNull
	private String uri;
	@ApiModelProperty(required = true, value = "Email de contacto para la solución del problema")
    @NotNull
	private String contactEmail;	
	@ApiModelProperty(required = true, value = "timestamp del error")
	@NotNull
	private String timeStamp;
        

      public RespuestaError(EnumHttpStatus status, String businessMessage, String reasonPhrase, String cveMessage) {
		this.code = status.getCode().toString();
		this.description = status.getDescription();
		this.businessMessage = businessMessage;
		this.reasonPhrase = reasonPhrase;
		this.uri = "http://pmc.imss.gob.mx/help?cveMessage=" + cveMessage;

		SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");

		this.timeStamp = dateFormat.format(new Date());

	}	  


}
