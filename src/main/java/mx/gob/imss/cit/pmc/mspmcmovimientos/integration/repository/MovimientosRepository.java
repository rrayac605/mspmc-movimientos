package mx.gob.imss.cit.pmc.mspmcmovimientos.integration.repository;

import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import mx.gob.imss.cit.mspmccommons.integration.model.ArchivoDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.DatosModificadosDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.DetalleRegistroDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.MovimientosOutputDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.ResponseDTO;
import mx.gob.imss.cit.pmc.mspmcmovimientos.exception.BusinessException;

public interface MovimientosRepository {

	ResponseDTO<List<MovimientosOutputDTO>> findMovimientos(@Valid Integer cveDelegacion,
			@Valid Integer cveSubdelegacion, @Valid Integer cveTipoRiesgo, @Valid Integer cveConsecuencia,
			@Valid Integer cveCasoRegistro, @Valid String fromMonth, @Valid String fromYear, @Valid String toMonth,
			@Valid String toYear, @Valid Integer cveEstadoRegistro, @Valid List<Integer> cveEstadoRegistroList,
			@Valid String numNss, @Valid String refRegistroPatronal, @Valid String cveSituacionRegistro, Long page,
			Long totalElements, String origenAlta) throws BusinessException;

	DetalleRegistroDTO getDetalleMovimiento(@Valid String objectId, @Valid String numNss, @Valid Integer position,
			@Valid String numFolioMovtoOriginal);

	DetalleRegistroDTO getDetalleMovimiento(@Valid String objectId);
	
	Object updateMovimiento(DatosModificadosDTO input);

	Object updateMovimientoSusceptible(DetalleRegistroDTO input);

	DetalleRegistroDTO findOne(String objectId);
	
	void actualizaCifras(ArchivoDTO archivoDTO);
	
	Optional<ArchivoDTO> findOneById(String userId);

	void markAsPending(String objectId, Boolean isPending);
	
	Object updateMovimientoSinCambios(DatosModificadosDTO input);

}
