package mx.gob.imss.cit.pmc.mspmcmovimientos.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import mx.gob.imss.cit.mspmccommons.convert.ConvertNegIntResponse;
import mx.gob.imss.cit.mspmccommons.enums.AccionRegistroEnum;
import mx.gob.imss.cit.mspmccommons.enums.EstadoRegistroEnum;
import mx.gob.imss.cit.mspmccommons.enums.IdentificadorArchivoEnum;
import mx.gob.imss.cit.mspmccommons.enums.SituacionRegistroEnum;
import mx.gob.imss.cit.mspmccommons.integration.model.ArchivoDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.AuditoriaDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.CifrasControlDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.DatosModificadosDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.DetalleRegistroDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.ParametroDTO;
import mx.gob.imss.cit.mspmccommons.resp.DetalleRegistroResponse;
import mx.gob.imss.cit.mspmccommons.utils.DateUtils;
import mx.gob.imss.cit.pmc.mspmcmovimientos.exception.BusinessException;
import mx.gob.imss.cit.pmc.mspmcmovimientos.integration.repository.MovimientosRepository;
import mx.gob.imss.cit.pmc.mspmcmovimientos.integration.repository.ParametroRepository;
import mx.gob.imss.cit.pmc.mspmcmovimientos.service.MovimientosService;

@Service
public class MovimientosServiceImpl implements MovimientosService {

	private static final Logger logger = LoggerFactory.getLogger(MovimientosServiceImpl.class);

	@Autowired
	private MovimientosRepository movimientosRepository;
	
	@Autowired
	private ParametroRepository parametroRepository;

	@Override
	public Object findMovimientos(@Valid Integer cveDelegacion, @Valid Integer cveSubdelegacion,
			@Valid Integer cveTipoRiesgo, @Valid Integer cveConsecuencia, @Valid Integer cveCasoRegistro,
			@Valid String fromMonth, @Valid String fromYear, @Valid String toMonth, @Valid String toYear,
			@Valid Integer cveEstadoRegistro, @Valid List<Integer> cveEstadoRegistroList, @Valid String numNss,
			@Valid String refRegistroPatronal, @Valid String cveSituacionRegistro, Long page, Long totalElements,
			String origenAlta) throws BusinessException {

		return movimientosRepository.findMovimientos(cveDelegacion, cveSubdelegacion, cveTipoRiesgo, cveConsecuencia,
				cveCasoRegistro, fromMonth, fromYear, toMonth, toYear, cveEstadoRegistro, cveEstadoRegistroList, numNss,
				refRegistroPatronal, cveSituacionRegistro, page, totalElements, origenAlta);
	}

	@Override
	public DetalleRegistroResponse getDetalleMovimiento(@Valid String objectId, @Valid String numNss,
			@Valid Integer position, @Valid String numFolioMovtoOriginal) throws BusinessException {
		
		ConvertNegIntResponse converNegIntResp = new ConvertNegIntResponse();
		DetalleRegistroDTO detalleDTO = movimientosRepository.getDetalleMovimiento(objectId, numNss, position, numFolioMovtoOriginal);
		DetalleRegistroResponse detalleResp = converNegIntResp.getDetalleResp(detalleDTO);
		return detalleResp;
		
	}

	@Override
	public Object updateCambios(DatosModificadosDTO input) throws BusinessException {
		return movimientosRepository.updateMovimiento(input);
	}

	@Override
	public void updateSusceptibles(List<String> input) throws BusinessException {
		for (String objectId : input) {
			logger.debug("El objectId es: {}", objectId);
			DetalleRegistroDTO detalle = movimientosRepository.findOne(objectId);
			logger.debug("Se encontro el archivo: {}", detalle);
			if (!Arrays.asList(4, 8, 3, 7).contains(detalle.getAseguradoDTO().getCveEstadoRegistro())
					&& detalle.getAseguradoDTO() != null) {
				detalle.setAuditorias(crearBitacoras(detalle));
				Optional<ArchivoDTO> optional = movimientosRepository
						.findOneById(detalle.getIdentificadorArchivo().toString());
				ArchivoDTO archivoDTO = optional.get();
				CifrasControlDTO cifras = archivoDTO.getCifrasControlDTO();
				actualizarCifras(cifras, detalle);
				if (Arrays.asList(1, 2).contains(detalle.getAseguradoDTO().getCveEstadoRegistro())) {
					detalle.getAseguradoDTO()
							.setCveEstadoRegistro(EstadoRegistroEnum.SUSCEPTIBLE.getCveEstadoRegistro());
					detalle.getAseguradoDTO().setDesEstadoRegistro(EstadoRegistroEnum.SUSCEPTIBLE.getDesDescripcion());
				} else {
					detalle.getAseguradoDTO()
							.setCveEstadoRegistro(EstadoRegistroEnum.SUSCEPTIBLE_OTRAS.getCveEstadoRegistro());
					detalle.getAseguradoDTO()
							.setDesEstadoRegistro(EstadoRegistroEnum.SUSCEPTIBLE_OTRAS.getDesDescripcion());
				}
				movimientosRepository.actualizaCifras(archivoDTO);
				movimientosRepository.updateMovimientoSusceptible(detalle);
			}			
		}
	}

	@Override
	public void updateCorrectos(List<String> input) throws BusinessException {
		for (String objectId : input) {
			logger.debug("El objectId es: {}", objectId);
			DetalleRegistroDTO detalle = movimientosRepository.findOne(objectId);
			logger.debug("Se encontro el archivo: {}", detalle);
			if (!Arrays.asList(1, 2, 3, 5, 6, 7).contains(detalle.getAseguradoDTO().getCveEstadoRegistro())
					&& detalle.getAseguradoDTO() != null) {
				detalle.setAuditorias(crearBitacoras(detalle));
				Optional<ArchivoDTO> optional = movimientosRepository
						.findOneById(detalle.getIdentificadorArchivo().toString());
				ArchivoDTO archivoDTO = optional.get();
				CifrasControlDTO cifras = archivoDTO.getCifrasControlDTO();
				actualizarCifrasCorrectos(cifras, detalle);
				if (Arrays.asList(4, 2).contains(detalle.getAseguradoDTO().getCveEstadoRegistro())) {
					if (detalle.getAseguradoDTO().getCveCodigoError() != null
							&& detalle.getAseguradoDTO().getCveCodigoError() != "0") {
						detalle.getAseguradoDTO()
								.setCveEstadoRegistro(EstadoRegistroEnum.ERRONEO.getCveEstadoRegistro());
						detalle.getAseguradoDTO().setDesEstadoRegistro(EstadoRegistroEnum.ERRONEO.getDesDescripcion());
					} else {
						detalle.getAseguradoDTO()
								.setCveEstadoRegistro(EstadoRegistroEnum.CORRECTO.getCveEstadoRegistro());
						detalle.getAseguradoDTO().setDesEstadoRegistro(EstadoRegistroEnum.CORRECTO.getDesDescripcion());
					}
				} else {
					if (detalle.getAseguradoDTO().getCveCodigoError() != null
							&& detalle.getAseguradoDTO().getCveCodigoError() != "0") {
						detalle.getAseguradoDTO()
								.setCveEstadoRegistro(EstadoRegistroEnum.ERRONEO_OTRAS.getCveEstadoRegistro());
						detalle.getAseguradoDTO()
								.setDesEstadoRegistro(EstadoRegistroEnum.ERRONEO_OTRAS.getDesDescripcion());
					} else {
						detalle.getAseguradoDTO()
								.setCveEstadoRegistro(EstadoRegistroEnum.CORRECTO_OTRAS.getCveEstadoRegistro());
						detalle.getAseguradoDTO()
								.setDesEstadoRegistro(EstadoRegistroEnum.CORRECTO_OTRAS.getDesDescripcion());
					}
				}
				if (detalle.getAseguradoDTO().getCveEstadoRegistro()
						.equals(EstadoRegistroEnum.ERRONEO.getCveEstadoRegistro())
						|| detalle.getAseguradoDTO().getCveEstadoRegistro()
								.equals(EstadoRegistroEnum.ERRONEO_OTRAS.getCveEstadoRegistro())) {
					Optional<ParametroDTO> urlUpdateMovto = null;
					if (detalle.getCveOrigenArchivo()
							.equals(IdentificadorArchivoEnum.ARCHIVO_NSSA.getIdentificador())) {
						urlUpdateMovto = parametroRepository.findOneByCve("urlValidaMovimientoNssa");
					} else if (detalle.getCveOrigenArchivo()
							.equals(IdentificadorArchivoEnum.ARCHIVO_SISAT.getIdentificador())) {
						urlUpdateMovto = parametroRepository.findOneByCve("urlValidaMovimientoSisat");
					}
					HttpEntity<String> request = new HttpEntity<String>(objectId);
					RestTemplate restTemplate = null;
					restTemplate = new RestTemplate();
					restTemplate.postForObject(urlUpdateMovto.get().getDesParametro(), request, String.class);
				} else {
					movimientosRepository.updateMovimientoSusceptible(detalle);
				}
				movimientosRepository.actualizaCifras(archivoDTO);
			}
		}
	}

	private void actualizarCifras(CifrasControlDTO cifrasControlDTO, DetalleRegistroDTO detalle) {

		switch (detalle.getAseguradoDTO().getCveEstadoRegistro()) {
		case 1:
			cifrasControlDTO.setNumRegistrosCorrectos(cifrasControlDTO.getNumRegistrosCorrectos() - 1);
			cifrasControlDTO.setNumRegistrosSus(cifrasControlDTO.getNumRegistrosSus() + 1);
			break;
		case 2:
			cifrasControlDTO.setNumRegistrosError(cifrasControlDTO.getNumRegistrosError() - 1);
			cifrasControlDTO.setNumRegistrosSus(cifrasControlDTO.getNumRegistrosSus() + 1);
			break;
		case 3:
			cifrasControlDTO.setNumRegistrosDup(cifrasControlDTO.getNumRegistrosDup() - 1);
			cifrasControlDTO.setNumRegistrosSus(cifrasControlDTO.getNumRegistrosSus() + 1);
			break;
		case 5:
			cifrasControlDTO.setNumRegistrosCorrectosOtras(cifrasControlDTO.getNumRegistrosCorrectosOtras() - 1);
			cifrasControlDTO.setNumRegistrosSusOtras(cifrasControlDTO.getNumRegistrosSusOtras() + 1);
			break;
		case 6:
			cifrasControlDTO.setNumRegistrosErrorOtras(cifrasControlDTO.getNumRegistrosErrorOtras() - 1);
			cifrasControlDTO.setNumRegistrosSusOtras(cifrasControlDTO.getNumRegistrosSusOtras() + 1);
			break;
		case 7:
			cifrasControlDTO.setNumRegistrosDupOtras(cifrasControlDTO.getNumRegistrosDupOtras() - 1);
			cifrasControlDTO.setNumRegistrosSusOtras(cifrasControlDTO.getNumRegistrosSusOtras() + 1);
			break;
		default:
			break;
		}

	}

	private void actualizarCifrasCorrectos(CifrasControlDTO cifrasControlDTO, DetalleRegistroDTO detalle) {

		switch (detalle.getAseguradoDTO().getCveEstadoRegistro()) {
		case 4:
			cifrasControlDTO.setNumRegistrosSus(cifrasControlDTO.getNumRegistrosSus() - 1);
			cifrasControlDTO.setNumRegistrosCorrectos(cifrasControlDTO.getNumRegistrosCorrectos() + 1);
			break;
		case 2:
			cifrasControlDTO.setNumRegistrosError(cifrasControlDTO.getNumRegistrosError() - 1);
			cifrasControlDTO.setNumRegistrosCorrectos(cifrasControlDTO.getNumRegistrosCorrectos() + 1);
			break;
		case 3:
			cifrasControlDTO.setNumRegistrosDup(cifrasControlDTO.getNumRegistrosDup() - 1);
			cifrasControlDTO.setNumRegistrosCorrectos(cifrasControlDTO.getNumRegistrosCorrectos() + 1);
			break;
		case 8:
			cifrasControlDTO.setNumRegistrosSusOtras(cifrasControlDTO.getNumRegistrosSusOtras() - 1);
			cifrasControlDTO.setNumRegistrosCorrectosOtras(cifrasControlDTO.getNumRegistrosCorrectosOtras() + 1);
			break;
		case 6:
			cifrasControlDTO.setNumRegistrosErrorOtras(cifrasControlDTO.getNumRegistrosErrorOtras() - 1);
			cifrasControlDTO.setNumRegistrosCorrectosOtras(cifrasControlDTO.getNumRegistrosCorrectosOtras() + 1);
			break;
		case 7:
			cifrasControlDTO.setNumRegistrosDupOtras(cifrasControlDTO.getNumRegistrosDupOtras() - 1);
			cifrasControlDTO.setNumRegistrosCorrectosOtras(cifrasControlDTO.getNumRegistrosCorrectosOtras() + 1);
			break;
		default:
			break;
		}

	}

	private List<AuditoriaDTO> crearBitacoras(DetalleRegistroDTO registro) {
		List<AuditoriaDTO> auditorias = new ArrayList<>();
		AuditoriaDTO auditoria = new AuditoriaDTO();
		auditoria.setFecAlta(DateUtils.getSysDateMongoISO());
		auditoria.setFecBaja(DateUtils.getSysDateMongoISO());
		auditoria.setNomUsuario(auditoria.getNomUsuario());
		auditoria.setNumFolioMovOriginal(registro.getAseguradoDTO().getRefFolioOriginal());
		auditoria.setCveIdAccionRegistro(AccionRegistroEnum.MODIFICACION.getClave());
		auditoria.setDesAccionRegistro(AccionRegistroEnum.MODIFICACION.getDescripcion());
		auditoria.setCveSituacionRegistro(SituacionRegistroEnum.PENDIENTE.getClave());
		auditoria.setDesSituacionRegistro(SituacionRegistroEnum.PENDIENTE.getDescripcion());
		AuditoriaDTO auditoria1 = new AuditoriaDTO();
		auditoria1.setFecAlta(DateUtils.getSysDateMongoISO());
		auditoria1.setFecBaja(DateUtils.getSysDateMongoISO());
		auditoria1.setNomUsuario(auditoria.getNomUsuario());
		auditoria1.setNumFolioMovOriginal(registro.getAseguradoDTO().getRefFolioOriginal());
		auditoria1.setCveIdAccionRegistro(AccionRegistroEnum.MODIFICACION.getClave());
		auditoria1.setDesAccionRegistro(AccionRegistroEnum.MODIFICACION.getDescripcion());
		auditoria1.setCveSituacionRegistro(SituacionRegistroEnum.APROBADO.getClave());
		auditoria1.setDesSituacionRegistro(SituacionRegistroEnum.APROBADO.getDescripcion());
		auditorias.add(auditoria);
		auditorias.add(auditoria1);
		return auditorias;
	}

	@Override
	public void markAsPending(String objectId, Boolean isPending) {
		movimientosRepository.markAsPending(objectId, isPending);
	}
	
	@Override
	public DetalleRegistroResponse getDetalleMovimiento(@Valid String objectId) throws BusinessException {
		ConvertNegIntResponse converNegIntResp = new ConvertNegIntResponse();
		DetalleRegistroDTO detalleDTO = movimientosRepository.getDetalleMovimiento(objectId);
		DetalleRegistroResponse detalleResp = converNegIntResp.getDetalleResp(detalleDTO);
		return detalleResp;
	}
	
	@Override
	public Object confirmarSinCambios(DatosModificadosDTO input) throws BusinessException {
		return movimientosRepository.updateMovimientoSinCambios(input);
	}

}
