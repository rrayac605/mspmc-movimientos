package mx.gob.imss.cit.pmc.mspmcmovimientos.controller;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import mx.gob.imss.cit.mspmccommons.dto.ErrorResponse;
import mx.gob.imss.cit.mspmccommons.integration.model.DatosModificadosDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.PaginaMovimientos;
import mx.gob.imss.cit.mspmccommons.model.ModelVersion;
import mx.gob.imss.cit.mspmccommons.resp.DetalleRegistroResponse;
import mx.gob.imss.cit.pmc.mspmcmovimientos.exception.BusinessException;
import mx.gob.imss.cit.pmc.mspmcmovimientos.model.RespuestaError;
import mx.gob.imss.cit.pmc.mspmcmovimientos.service.MovimientosService;

@RestController
@Api(value = "Movimientos PMC", tags = { "Movimientos PMC Rest" })
@RequestMapping("/msmovimientos/v1")
public class MovimientosController {

	private static final String SUCCESSFULLY_UPDATED = "Se actualizó el movimiento exitosametne";

	@Autowired
	private MovimientosService movimientosService;
	
	private final static String VERSION_SERVICE = "mspmc-cambios-1.0.9";
	
	private final static String FOLIO_SERVICE = "INC129170";
	
	private final static String NOTA_SERVICE = "Inconveniente falla reporte";

	@ApiOperation(value = "Consulta movimientos", nickname = "totalmovimientossGet", notes = "Consulta movimientos", response = PaginaMovimientos.class, responseContainer = "List", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa", response = PaginaMovimientos.class, responseContainer = "List"),
			@ApiResponse(code = 204, message = "Sin resultados", response = PaginaMovimientos.class),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = RespuestaError.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@GetMapping(value = "/movimientos", produces = { "application/json" })
	public ResponseEntity<Object> movimientosGet(
			@ApiParam(value = "Clave de la Delegacion") @Valid @RequestParam(value = "cveDelegacion", required = false) Integer cveDelegacion,
			@ApiParam(value = "Clave de la subdelegacion") @Valid @RequestParam(value = "cveSubdelegacion", required = false) Integer cveSubdelegacion,
			@ApiParam(value = "Clave Tipo Riesgo") @Valid @RequestParam(value = "cveTipoRiesgo", required = false) Integer cveTipoRiesgo,
			@ApiParam(value = "Clave consecuencia") @Valid @RequestParam(value = "cveConsecuencia", required = false) Integer cveConsecuencia,
			@ApiParam(value = "Clave caso del regisgtro") @Valid @RequestParam(value = "cveCasoRegistro", required = false) Integer cveCasoRegistro,
			@ApiParam(value = "Mes inicio consulta") @Valid @RequestParam(value = "fromMonth", required = true) String fromMonth,
			@ApiParam(value = "Anio inicio consulta") @Valid @RequestParam(value = "fromYear", required = true) String fromYear,
			@ApiParam(value = "Mes fin consulta") @Valid @RequestParam(value = "toMonth", required = true) String toMonth,
			@ApiParam(value = "Anio fin consulta") @Valid @RequestParam(value = "toYear", required = true) String toYear,
			@ApiParam(value = "Clave del estado del registro") @Valid @RequestParam(value = "cveEstadoRegistro", required = false) Integer cveEstadoRegistro,
			@ApiParam(value = "Lista de claves del estado del registro") @Valid @RequestParam(value = "cveEstadoRegistroList", required = false) List<Integer> cveEstadoRegistroList,
			@ApiParam(value = "NSS") @Valid @RequestParam(value = "numNss", required = false) String numNss,
			@ApiParam(value = "Registro Patronal") @Valid @RequestParam(value = "refRegistroPatronal", required = false) String refRegistroPatronal,
			@ApiParam(value = "Situacion del registro") @Valid @RequestParam(value = "cveSituacionRegistro", required = false) String cveSituacionRegistro,
			@ApiParam(value = "Pagina que se desea consultar") @Valid @RequestParam(value = "page", required = false) Long page,
			@ApiParam(value = "Elementos totales de la busqueda, usado para saber si ejecutar el conteo o no")
			@Valid @RequestParam(value = "totalElements", required = false) Long totalElements,
			@ApiParam(value = "Cadena de caracteres que representa el tipo de usuario que realizo la peticion")
			@Valid @RequestParam(value = "origenAlta", required = false) String origenAlta) {

		ResponseEntity<Object> responseEntity = null;

		try {

			Object findMovimientos = movimientosService.findMovimientos(cveDelegacion, cveSubdelegacion, cveTipoRiesgo,
					cveConsecuencia, cveCasoRegistro, fromMonth, fromYear, toMonth, toYear, cveEstadoRegistro,
					cveEstadoRegistroList, numNss, refRegistroPatronal, cveSituacionRegistro, page, totalElements, origenAlta);
			if (findMovimientos != null) {
				return new ResponseEntity<>(findMovimientos, HttpStatus.PARTIAL_CONTENT);
			} else {
				return new ResponseEntity<>(findMovimientos, HttpStatus.NO_CONTENT);
			}
		} catch (BusinessException be) {
			responseEntity = buildBEResponse(be);
		}
		return responseEntity;

	}

	@ApiOperation(value = "Consulta detalle movimientos", nickname = "detallemovimientossGet", notes = "Consulta movimientos", response = PaginaMovimientos.class, responseContainer = "List", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa", response = PaginaMovimientos.class, responseContainer = "List"),
			@ApiResponse(code = 204, message = "Sin resultados", response = PaginaMovimientos.class),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = RespuestaError.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@GetMapping(value = "/detallemovimientos", produces = { "application/json" })
	public ResponseEntity<Object> movimientoDetalle(
			@ApiParam(value = "ObjectId") @Valid @RequestParam(value = "objectId", required = true) String objectId,
			@Valid @RequestParam(value = "numNss", required = true) String numNss,
			@ApiParam(value = "Posicion detalle registro") @Valid @RequestParam(value = "position", required = true) Integer position,
			@ApiParam(value = "Num Folio Movimiento Original") @Valid @RequestParam(value = "numFolioMovtoOriginal", required = true) String numFolioMovtoOriginal) {

		ResponseEntity<Object> responseEntity = null;

		try {

			DetalleRegistroResponse findMovimientos = movimientosService.getDetalleMovimiento(objectId, numNss, position,
					numFolioMovtoOriginal);
			if (findMovimientos != null) {
				return new ResponseEntity<>(findMovimientos, HttpStatus.PARTIAL_CONTENT);
			} else {
				return new ResponseEntity<>(findMovimientos, HttpStatus.NO_CONTENT);
			}
		} catch (BusinessException be) {
			responseEntity = buildBEResponse(be);
		}
		return responseEntity;

	}

	@ApiOperation(value = "Consulta detalle movimientos", nickname = "detallemovimientossGet", notes = "Consulta movimientos", response = PaginaMovimientos.class, responseContainer = "List", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa", response = PaginaMovimientos.class, responseContainer = "List"),
			@ApiResponse(code = 204, message = "Sin resultados", response = PaginaMovimientos.class),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = RespuestaError.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@GetMapping(value = "/detallemovimientos/objectId", produces = { "application/json" })
	public ResponseEntity<Object> movimientoDetalle(
			@ApiParam(value = "ObjectId") @Valid @RequestParam(value = "objectId", required = true) String objectId) {

		ResponseEntity<Object> responseEntity = null;

		try {

			DetalleRegistroResponse findMovimientos = movimientosService.getDetalleMovimiento(objectId);
			if (findMovimientos != null) {
				return new ResponseEntity<>(findMovimientos, HttpStatus.PARTIAL_CONTENT);
			} else {
				return new ResponseEntity<>(findMovimientos, HttpStatus.NO_CONTENT);
			}
		} catch (BusinessException be) {
			responseEntity = buildBEResponse(be);
		}
		return responseEntity;

	}
	
	@ApiOperation(value = "Actualizar campos modificados", nickname = "actualizacionCamposModificados", notes = "Actualizar campos modificado", response = PaginaMovimientos.class, responseContainer = "List", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa", response = PaginaMovimientos.class, responseContainer = "List"),
			@ApiResponse(code = 204, message = "Sin resultados", response = PaginaMovimientos.class),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = RespuestaError.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@PostMapping(value = "/movimientos", produces = { "application/json" })
	public ResponseEntity<Object> actualizaMovimientos(@RequestBody DatosModificadosDTO input) {

		ResponseEntity<Object> responseEntity = null;

		try {

			Object findMovimientos = movimientosService.updateCambios(input);
			if (findMovimientos.equals(true)) {
				return new ResponseEntity<>(SUCCESSFULLY_UPDATED, HttpStatus.OK);
			} else {
				return new ResponseEntity<>("No pudo ser posbile realizar la actualizacion", HttpStatus.CONFLICT);
			}
		} catch (BusinessException be) {
			responseEntity = buildBEResponse(be);
		}
		return responseEntity;

	}

	@ApiOperation(value = "Actualizar campos modificados", nickname = "actualizacionCamposModificados", notes = "Actualizar campos modificado", response = PaginaMovimientos.class, responseContainer = "List", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa", response = PaginaMovimientos.class, responseContainer = "List"),
			@ApiResponse(code = 204, message = "Sin resultados", response = PaginaMovimientos.class),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = RespuestaError.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@PostMapping(value = "/confirmarSinCambio", produces = { "application/json" })
	public ResponseEntity<Object> confirmarSinCambio(@RequestBody DatosModificadosDTO input) {

		ResponseEntity<Object> responseEntity = null;

		try {
			Object findMovimientos = movimientosService.confirmarSinCambios(input);
			if (findMovimientos.equals(true)) {
				return new ResponseEntity<>(SUCCESSFULLY_UPDATED, HttpStatus.OK);
			} else {
				return new ResponseEntity<>("No pudo ser posbile realizar la actualizacion", HttpStatus.CONFLICT);
			}
		} catch (BusinessException be) {
			responseEntity = buildBEResponse(be);
		}
		return responseEntity;

	}
	
	@ApiOperation(value = "Actualizar campos modificados", nickname = "actualizacionCamposModificados", notes = "Actualizar campos modificado", response = PaginaMovimientos.class, responseContainer = "List", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa", response = PaginaMovimientos.class, responseContainer = "List"),
			@ApiResponse(code = 204, message = "Sin resultados", response = PaginaMovimientos.class),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = RespuestaError.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@PostMapping(value = "/actualizaEstado", produces = { "application/json" })
	public ResponseEntity<Object> actualizaEstado(@RequestBody List<String> input) {

		ResponseEntity<Object> responseEntity = null;

		try {

			movimientosService.updateSusceptibles(input);
			return new ResponseEntity<>(SUCCESSFULLY_UPDATED, HttpStatus.OK);
		} catch (BusinessException be) {
			responseEntity = buildBEResponse(be);
		}
		return responseEntity;

	}
	
	@ApiOperation(value = "Actualizar campos modificados", nickname = "actualizacionCamposModificados", notes = "Actualizar campos modificado", response = PaginaMovimientos.class, responseContainer = "List", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa", response = PaginaMovimientos.class, responseContainer = "List"),
			@ApiResponse(code = 204, message = "Sin resultados", response = PaginaMovimientos.class),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = RespuestaError.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@PostMapping(value = "/actualizaEstadoCorrecto", produces = { "application/json" })
	public ResponseEntity<Object> actualizaEstadoCorrecto(@RequestBody List<String> input) {

		ResponseEntity<Object> responseEntity = null;

		try {

			movimientosService.updateCorrectos(input);
			return new ResponseEntity<>(SUCCESSFULLY_UPDATED, HttpStatus.OK);
		} catch (BusinessException be) {
			responseEntity = buildBEResponse(be);
		}
		return responseEntity;

	}

	@ApiOperation(value = "Marca un movimiento como con cambios pendientes", nickname = "markAsPending", notes = "Marca registro como pendiente")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa"),
			@ApiResponse(code = 204, message = "Sin resultados"),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = RespuestaError.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@GetMapping(value = "/markAsPending", produces = { "application/json" })
	public ResponseEntity<String> markAsPending(@ApiParam(value = "Id del registro a marcar")
			@Valid @RequestParam(value = "objectId", required = true) String objectId,
			@Valid @RequestParam(value = "isPending", required = true) Boolean isPending) {
		ResponseEntity<String> responseEntity = null;
		try {
			movimientosService.markAsPending(objectId, isPending);
			return new ResponseEntity<>("Se marco el movimiento como pendiente exitosametne", HttpStatus.OK);
		} catch (Exception be) {
			be.printStackTrace();
			responseEntity = new ResponseEntity<>("Ocurrio un error, vuelva a intentar mas tarde",
					HttpStatus.SERVICE_UNAVAILABLE);
		}
		return responseEntity;

	}

	private ResponseEntity<Object> buildBEResponse(BusinessException be) {
		List<RespuestaError> respuestas = new ArrayList<>();
		int numberHTTPDesired = Integer.parseInt(be.getRespuestaError().getCode());
		RespuestaError respuestaError = be.getRespuestaError();
		respuestas.add(respuestaError);
		return new ResponseEntity<>(respuestas, HttpStatus.valueOf(numberHTTPDesired));
	}

	@ApiOperation(value = "version", nickname = "version", notes = "version", response = Object.class, responseContainer = "binary", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa", response = ResponseEntity.class, responseContainer = "byte"),
			@ApiResponse(code = 204, message = "Sin resultados", response = ResponseEntity.class),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = ErrorResponse.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@GetMapping(value = "/version")
	public ModelVersion version() throws Exception {
		return new ModelVersion(VERSION_SERVICE, FOLIO_SERVICE, NOTA_SERVICE);
	}	
	
	@ApiOperation(value = "versionCommons", nickname = "versionCommons", notes = "versionCommons", response = Object.class, responseContainer = "binary", tags = {})
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Respuesta exitosa", response = ResponseEntity.class, responseContainer = "byte"),
			@ApiResponse(code = 204, message = "Sin resultados", response = ResponseEntity.class),
			@ApiResponse(code = 500, message = "Describe un error general del sistema", response = ErrorResponse.class) })
	@CrossOrigin(origins = "*", allowedHeaders = "*")
	@GetMapping(value = "/versionCommons")
	public ModelVersion versionCommons() throws Exception {
		return new ModelVersion();
	}

}
