package mx.gob.imss.cit.pmc.mspmcmovimientos.service;

import java.util.List;

import javax.validation.Valid;

import mx.gob.imss.cit.mspmccommons.integration.model.DatosModificadosDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.DetalleRegistroDTO;
import mx.gob.imss.cit.mspmccommons.resp.DetalleRegistroResponse;
import mx.gob.imss.cit.pmc.mspmcmovimientos.exception.BusinessException;

public interface MovimientosService {

	Object findMovimientos(@Valid Integer cveDelegacion, @Valid Integer cveSubdelegacion, @Valid Integer cveTipoRiesgo,
			@Valid Integer cveConsecuencia, @Valid Integer cveCasoRegistro, @Valid String fromMonth,
			@Valid String fromYear, @Valid String toMonth, @Valid String toYear, @Valid Integer cveEstadoRegistro,
			@Valid List<Integer> cveEstadoRegistroList, @Valid String numNss, @Valid String refRegistroPatronal,
			@Valid String cveSituacionRegistro, Long page, Long totalElements, String origenAlta)
			throws BusinessException;

	DetalleRegistroResponse getDetalleMovimiento(@Valid String objectId, @Valid String numNss, @Valid Integer position,
			@Valid String numFolioMovtoOriginal) throws BusinessException;

	Object updateCambios(DatosModificadosDTO input) throws BusinessException;

	Object confirmarSinCambios(DatosModificadosDTO input) throws BusinessException;
	
	void updateSusceptibles(List<String> input) throws BusinessException;

	void updateCorrectos(List<String> input) throws BusinessException;

	void markAsPending(String objectId, Boolean isPending);
	
	DetalleRegistroResponse getDetalleMovimiento(@Valid String objectId) throws BusinessException;

}
