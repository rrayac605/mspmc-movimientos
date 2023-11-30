package mx.gob.imss.cit.pmc.mspmcmovimientos.integration.repository.impl;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.validation.Valid;

import org.bson.types.ObjectId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.CountOperation;
import org.springframework.data.mongodb.core.aggregation.LimitOperation;
import org.springframework.data.mongodb.core.aggregation.LookupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.aggregation.SkipOperation;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import org.springframework.data.mongodb.core.aggregation.UnwindOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Repository;

import mx.gob.imss.cit.mspmccommons.dto.BitacoraDictamenDTO;
import mx.gob.imss.cit.mspmccommons.dto.CambioDTO;
import mx.gob.imss.cit.mspmccommons.dto.CasoRegistroDTO;
import mx.gob.imss.cit.mspmccommons.enums.AccionRegistroEnum;
import mx.gob.imss.cit.mspmccommons.enums.EstadoRegistroEnum;
import mx.gob.imss.cit.mspmccommons.enums.SituacionRegistroEnum;
import mx.gob.imss.cit.mspmccommons.integration.model.ArchivoDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.AuditoriaDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.CountDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.DatosModificadosDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.DetalleRegistroDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.MovimientosOutputDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.ParametroDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.ResponseDTO;
import mx.gob.imss.cit.mspmccommons.utils.AggregationUtils;
import mx.gob.imss.cit.mspmccommons.utils.CustomAggregationOperation;
import mx.gob.imss.cit.mspmccommons.utils.DateUtils;
import mx.gob.imss.cit.mspmccommons.utils.NumberUtils;
import mx.gob.imss.cit.mspmccommons.utils.ObjectUtils;
import mx.gob.imss.cit.mspmccommons.utils.StringUtils;
import mx.gob.imss.cit.pmc.mspmcmovimientos.exception.BusinessException;
import mx.gob.imss.cit.pmc.mspmcmovimientos.integration.repository.MovimientosRepository;
import mx.gob.imss.cit.pmc.mspmcmovimientos.integration.repository.ParametroRepository;

@Repository("movimientosService")
public class MovimientosRepositoryImpl implements MovimientosRepository {

	private static final Logger logger = LoggerFactory.getLogger(MovimientosRepositoryImpl.class);

	private static final String RFC_IMSS = "IMS421231I45";
	private static final String IDENTIFICADOR_ARCHIVO = "identificadorArchivo";
	private static final String ARCHIVO_DTO = "archivoDTO";
	private static final String MCT_ARCHIVO = "MCT_ARCHIVO";

	@Autowired
	MongoOperations mongoOperations;

	@Autowired
	ParametroRepository parametroRepository;

	@Override
	public ResponseDTO<List<MovimientosOutputDTO>> findMovimientos(@Valid Integer cveDelegacion,
			@Valid Integer cveSubdelegacion, @Valid Integer cveTipoRiesgo, @Valid Integer cveConsecuencia,
			@Valid Integer cveCasoRegistro, @Valid String fromMonth, @Valid String fromYear, @Valid String toMonth,
			@Valid String toYear, @Valid Integer cveEstadoRegistro, @Valid List<Integer> cveEstadoRegistroList,
			@Valid String numNss, @Valid String refRegistroPatronal, @Valid String cveSituacionRegistro, Long page,
			Long totalElements, String origenAlta) throws BusinessException {

		// Se calculan las fechas inicio y fin para la consulta
		Date fecProcesoIni = DateUtils.calculateBeginDate(fromYear, fromMonth, null);
		Date fecProcesoFin = DateUtils.calculateEndDate(toYear, toMonth, null);
		Criteria cFecProcesoCarga = null;

		if (fecProcesoIni != null && fecProcesoFin != null) {
			cFecProcesoCarga = new Criteria().andOperator(
					Criteria.where("archivoDTO.fecProcesoCarga").gt(fecProcesoIni),
					Criteria.where("archivoDTO.fecProcesoCarga").lte(fecProcesoFin));
		}
		logger.info("cveDelegacion recibida: " + cveDelegacion);
		logger.info("cveSubdelegacion reciibda: " + cveSubdelegacion);
		// Criterios de archivo
		Criteria cDelAndSubDel = null;
		Criteria cDel = null;
		Criteria cCveTipoRiesgo = null;
		Criteria cCveConsecuencia = null;
		Criteria cCveCasoRegistro = null;
		Criteria cCveEstadoRegistroList = null;
		Criteria cNumNss = null;
		Criteria cRefRegistroPatronal = null;
		// Criterios de detalle archivo
		Criteria cCveEstadoArchivo = null;
		Criteria cDesRfc = null;

		cCveEstadoArchivo = Criteria.where("archivoDTO.cveEstadoArchivo").is("2");

		if (cveDelegacion != null && cveDelegacion > 0 && cveSubdelegacion != null && cveSubdelegacion > 0) {
			Criteria delAsegurado = Criteria.where("aseguradoDTO.cveDelegacionNss").is(cveDelegacion);
			Criteria subdelAsegurado = Criteria.where("aseguradoDTO.cveSubdelNss").is(cveSubdelegacion);

			Criteria delPatron = Criteria.where("patronDTO.cveDelRegPatronal").is(cveDelegacion);
			Criteria subdelPatron = Criteria.where("patronDTO.cveSubDelRegPatronal").is(cveSubdelegacion);

			cDelAndSubDel = new Criteria().orOperator(new Criteria().andOperator(delAsegurado, subdelAsegurado),
					new Criteria().andOperator(delPatron, subdelPatron));

		} else if ((cveDelegacion != null && cveDelegacion > 0)
				&& (cveSubdelegacion == null || cveSubdelegacion == 0)) {
			Criteria delAsegurado = Criteria.where("aseguradoDTO.cveDelegacionNss").is(cveDelegacion);
			Criteria delPatron = Criteria.where("patronDTO.cveDelRegPatronal").is(cveDelegacion);
			cDel = new Criteria().orOperator(delAsegurado, delPatron);
		}

		if (cveTipoRiesgo != null && cveTipoRiesgo > 0) {
			cCveTipoRiesgo = Criteria.where("incapacidadDTO.cveTipoRiesgo").is(cveTipoRiesgo);
		}
		if (cveConsecuencia != null && cveConsecuencia >= 0) {
			cCveConsecuencia = Criteria.where("incapacidadDTO.cveConsecuencia").is(cveConsecuencia);
		}
		if (cveCasoRegistro != null && cveCasoRegistro > 0) {
			cCveCasoRegistro = Criteria.where("aseguradoDTO.cveCasoRegistro").is(cveCasoRegistro);
		}

		if (cveEstadoRegistroList != null && !cveEstadoRegistroList.isEmpty()) {
			cCveEstadoRegistroList = Criteria.where("aseguradoDTO.cveEstadoRegistro").in(cveEstadoRegistroList);

		}
		if (StringUtils.isNotBlank(numNss) && StringUtils.isNotEmpty(numNss)) {
			cNumNss = Criteria.where("aseguradoDTO.numNss").is(numNss);
		}
		if (StringUtils.isNotBlank(refRegistroPatronal) && StringUtils.isNotEmpty(refRegistroPatronal)) {
			cRefRegistroPatronal = Criteria.where("patronDTO.refRegistroPatronal").is(refRegistroPatronal);
		}

		if (StringUtils.isNotBlank(origenAlta) && StringUtils.isNotEmpty(origenAlta) && origenAlta.equals("EP")) {
			cDesRfc = Criteria.where("patronDTO.desRfc").is(RFC_IMSS);
		}

		Optional<ParametroDTO> parametro = parametroRepository.findOneByCve("elementsPaginator");
		Long size = Long.valueOf(parametro.get().getDesParametro());

		TypedAggregation<DetalleRegistroDTO> aggregation = buildAggregation(cFecProcesoCarga, cCveEstadoArchivo,
				cDelAndSubDel, cDel, cCveTipoRiesgo, cCveConsecuencia, cCveCasoRegistro, cCveEstadoRegistroList,
				cNumNss, cRefRegistroPatronal, cDesRfc, page, size);

		logger.info("--------------Query de agregacion-------------------");
		logger.info(aggregation.toString());
		AggregationResults<DetalleRegistroDTO> listArchivosAggregation = mongoOperations.aggregate(aggregation,
				DetalleRegistroDTO.class);
		logger.info("--------------Query de agregacion de conteo-------------------");
		Long count = totalElements;
		if (totalElements == null || (totalElements != null && totalElements <= 1L)) {
			TypedAggregation<CountDTO> countAggregation = buildCountAggregation(cFecProcesoCarga, cCveEstadoArchivo,
					cDelAndSubDel, cDel, cCveTipoRiesgo, cCveConsecuencia, cCveCasoRegistro, cCveEstadoRegistroList,
					cNumNss, cRefRegistroPatronal, cDesRfc);
			logger.info(countAggregation.toString());
			AggregationResults<CountDTO> countAggregationResult = mongoOperations.aggregate(countAggregation,
					DetalleRegistroDTO.class, CountDTO.class);
			count = !countAggregationResult.getMappedResults().isEmpty()
					&& countAggregationResult.getMappedResults().get(0) != null
							? countAggregationResult.getMappedResults().get(0).getTotalElements()
							: 0;
		}
		logger.info("Elemetos totales de la busqueda: " + count);
		logger.info("----------------------------------------------------");

		List<DetalleRegistroDTO> listArhivos = listArchivosAggregation.getMappedResults();

		listArhivos = listArhivos.stream().peek(movement -> {
			if (movement.getIncapacidadDTO() != null && movement.getIncapacidadDTO().getCveConsecuencia() != null
					&& movement.getIncapacidadDTO().getCveConsecuencia().equals("0")) {
				movement.getIncapacidadDTO().setDesConsecuencia("Sin consecuencias");
			}
		}).collect(Collectors.toList());

		ResponseDTO<List<MovimientosOutputDTO>> movimientosResponse = new ResponseDTO<>();
		if (!listArhivos.isEmpty()) {
			movimientosResponse.setData(llenarDatosMovimientos(listArhivos));
			movimientosResponse.setTotalElements(count);
			movimientosResponse.setTotalRows(Math.floorDiv(count, size));
			movimientosResponse.setPage(page != null ? page : 1L);
			movimientosResponse.setSize(size);
		}

		if (StringUtils.isNotBlank(cveSituacionRegistro) && StringUtils.isNotEmpty(cveSituacionRegistro)) {
			movimientosResponse.setData(new ArrayList<MovimientosOutputDTO>());
			movimientosResponse.setTotalRows(0L);
			movimientosResponse.setTotalElements(0L);
			return movimientosResponse;
		}

		return movimientosResponse;
	}

	private TypedAggregation<DetalleRegistroDTO> buildAggregation(Criteria cFecProcesoCarga, Criteria cCveEstadoArchivo,
			Criteria cDelAndSubDel, Criteria cDel, Criteria cCveTipoRiesgo, Criteria cCveConsecuencia,
			Criteria cCveCasoRegistro, Criteria cCveEstadoRegistroList, Criteria cNumNss, Criteria cRefRegistroPatronal,
			Criteria cDesRfc, Long page, Long size) {

		Long skipIndex = getSkipElements(page, size);
		LookupOperation lookup = Aggregation.lookup(MCT_ARCHIVO, IDENTIFICADOR_ARCHIVO, "_id", ARCHIVO_DTO);
		UnwindOperation unwind = Aggregation.unwind(ARCHIVO_DTO);
		LimitOperation limit = Aggregation.limit(size);
		SkipOperation skip = Aggregation.skip(skipIndex);
		Criteria cFecBaja = Criteria.where("aseguradoDTO.fecBaja").is(null);
		String jsonOpperaton = "{ $project: {" + "'objectIdArchivoDetalle': 1," + "'_idArchivo': 1,"
				+ "'aseguradoDTO.numIndice': 1," + "'aseguradoDTO.desEstadoRegistro': 1,"
				+ "'aseguradoDTO.cveEstadoRegistro': 1," + "'aseguradoDTO.refPrimerApellido': 1,"
				+ "'aseguradoDTO.refSegundoApellido': 1," + "'aseguradoDTO.nomAsegurado': 1,"
				+ "'aseguradoDTO.refFolioOriginal': 1," + "'aseguradoDTO.numNss': 1,"
				+ "'incapacidadDTO.desConsecuencia': 1," + "'incapacidadDTO.cveConsecuencia': 1,"
				+ "'incapacidadDTO.desTipoRiesgo': 1," + "'incapacidadDTO.cveTipoRiesgo': 1,"
				+ "'incapacidadDTO.fecFin': 1," + "'incapacidadDTO.fecInicio': 1,"
				+ "'incapacidadDTO.numDiasSubsidiados': 1,"
				+ "'incapacidadDTO.porPorcentajeIncapacidad': 1," + "'patronDTO.refRegistroPatronal': 1"
				+ "}}";
		CustomAggregationOperation projection = new CustomAggregationOperation(jsonOpperaton);
		// Las operaciones deben ir en orden en la lista
		List<AggregationOperation> aggregationOperationList = Arrays.asList(
				AggregationUtils.validateMatchOp(cCveTipoRiesgo), AggregationUtils.validateMatchOp(cCveConsecuencia),
				AggregationUtils.validateMatchOp(cCveCasoRegistro),
				AggregationUtils.validateMatchOp(cCveEstadoRegistroList), AggregationUtils.validateMatchOp(cNumNss),
				AggregationUtils.validateMatchOp(cRefRegistroPatronal), AggregationUtils.validateMatchOp(cFecBaja),
				AggregationUtils.validateMatchOp(cDelAndSubDel), AggregationUtils.validateMatchOp(cDel), lookup, unwind,
				AggregationUtils.validateMatchOp(cFecProcesoCarga), AggregationUtils.validateMatchOp(cCveEstadoArchivo),
				AggregationUtils.validateMatchOp(cDesRfc), skip, limit, projection);
		aggregationOperationList = aggregationOperationList.stream().filter(Objects::nonNull)
				.collect(Collectors.toList());
		return Aggregation.newAggregation(DetalleRegistroDTO.class, aggregationOperationList);
	}

	private TypedAggregation<CountDTO> buildCountAggregation(Criteria cFecProcesoCarga, Criteria cCveEstadoArchivo,
			Criteria cDelAndSubDel, Criteria cDel, Criteria cCveTipoRiesgo, Criteria cCveConsecuencia,
			Criteria cCveCasoRegistro, Criteria cCveEstadoRegistroList, Criteria cNumNss, Criteria cRefRegistroPatronal,
			Criteria cDesRfc) {

		LookupOperation lookup = Aggregation.lookup(MCT_ARCHIVO, IDENTIFICADOR_ARCHIVO, "_id", ARCHIVO_DTO);
		CountOperation count = Aggregation.count().as("totalElements");
		Criteria cFecBaja = Criteria.where("aseguradoDTO.fecBaja").is(null);
		// Las operaciones deben ir en orden en la lista
		List<AggregationOperation> aggregationOperationList = Arrays.asList(
				AggregationUtils.validateMatchOp(cCveTipoRiesgo), AggregationUtils.validateMatchOp(cCveConsecuencia),
				AggregationUtils.validateMatchOp(cCveCasoRegistro),
				AggregationUtils.validateMatchOp(cCveEstadoRegistroList), AggregationUtils.validateMatchOp(cNumNss),
				AggregationUtils.validateMatchOp(cRefRegistroPatronal), AggregationUtils.validateMatchOp(cFecBaja),
				AggregationUtils.validateMatchOp(cDelAndSubDel), AggregationUtils.validateMatchOp(cDel), lookup,
				AggregationUtils.validateMatchOp(cFecProcesoCarga), AggregationUtils.validateMatchOp(cCveEstadoArchivo),
				AggregationUtils.validateMatchOp(cDesRfc), count);
		aggregationOperationList = aggregationOperationList.stream().filter(Objects::nonNull)
				.collect(Collectors.toList());
		return Aggregation.newAggregation(CountDTO.class, aggregationOperationList);
	}

	private Long getSkipElements(Long page, Long size) {
		return page == null ? 0 : (page - 1) * size;
	}

	private List<MovimientosOutputDTO> llenarDatosMovimientos(List<DetalleRegistroDTO> listDetalleArhivos) {
		List<MovimientosOutputDTO> listMovimientos = null;
		if (listDetalleArhivos != null && !listDetalleArhivos.isEmpty()) {
			listMovimientos = new ArrayList<>();
			for (DetalleRegistroDTO detalleArchivoDTO : listDetalleArhivos) {
				MovimientosOutputDTO movimientosOutputDTO = new MovimientosOutputDTO();
				movimientosOutputDTO.setPosition(detalleArchivoDTO.getAseguradoDTO().getNumIndice());
				movimientosOutputDTO.setDesConsecuencia(detalleArchivoDTO.getIncapacidadDTO().getDesConsecuencia());
				movimientosOutputDTO.setCveConsecuencia(
						NumberUtils.safetyValidateInteger(detalleArchivoDTO.getIncapacidadDTO().getCveConsecuencia()));
				movimientosOutputDTO
						.setDesEstadoRegistro(validaCadena(detalleArchivoDTO.getAseguradoDTO().getDesEstadoRegistro()));
				movimientosOutputDTO.setCveEstadoRegistro(detalleArchivoDTO.getAseguradoDTO().getCveEstadoRegistro());
				movimientosOutputDTO.setDesTipoRiesgo(detalleArchivoDTO.getIncapacidadDTO().getDesTipoRiesgo());
				movimientosOutputDTO.setCveTipoRiesgo(
						NumberUtils.safetyParseInteger(detalleArchivoDTO.getIncapacidadDTO().getCveTipoRiesgo()));
				movimientosOutputDTO.setFecFin(StringUtils.safeValidateDate(detalleArchivoDTO.getIncapacidadDTO().getFecFin()));
				movimientosOutputDTO.setFecInicio(StringUtils.safeValidateDate(detalleArchivoDTO.getIncapacidadDTO().getFecInicio()));

				String nomCompleto = validaCadena(detalleArchivoDTO.getAseguradoDTO().getRefPrimerApellido()).concat(" ")
						.concat(validaCadena(detalleArchivoDTO.getAseguradoDTO().getRefSegundoApellido())).concat(" ")
						.concat(validaCadena(detalleArchivoDTO.getAseguradoDTO().getNomAsegurado()));
				movimientosOutputDTO.setNomCompletoAsegurado(nomCompleto);

				movimientosOutputDTO
						.setNumDiasSubsidiados(detalleArchivoDTO.getIncapacidadDTO().getNumDiasSubsidiados());
				movimientosOutputDTO.setNumFolioMovtoOriginal(
						validaCadena(detalleArchivoDTO.getAseguradoDTO().getRefFolioOriginal()));
				movimientosOutputDTO.setNumNss(validaCadena(detalleArchivoDTO.getAseguradoDTO().getNumNss()));
				movimientosOutputDTO.setPorcentajeIncapacidad(
						validaCadena(detalleArchivoDTO.getIncapacidadDTO().getPorPorcentajeIncapacidad().toString()));
				movimientosOutputDTO.setRefRegistroPatronal(
						validaCadena(detalleArchivoDTO.getPatronDTO().getRefRegistroPatronal()));
				movimientosOutputDTO.setObjectId(detalleArchivoDTO.getObjectIdArchivoDetalle().toString());
				movimientosOutputDTO.setObjectIdArchivo(
						StringUtils.safeValidateObjectId(detalleArchivoDTO.getIdentificadorArchivo()));
				listMovimientos.add(movimientosOutputDTO);

			}
			logger.info("Numero de archivos iterados: " + listDetalleArhivos.size());
		}

		return listMovimientos;
	}

	private String validaCadena(String cadena) {
		return StringUtils.isNotBlank(cadena) && StringUtils.isNotEmpty(cadena) ? cadena : "";
	}

	@Override
	public DetalleRegistroDTO getDetalleMovimiento(@Valid String objectId, @Valid String numNss,
			@Valid Integer position, @Valid String numFolioMovtoOriginal) {
		// Se construye la agregacion
		MatchOperation match = Aggregation.match(Criteria.where("_id").is(new ObjectId(objectId)));
		LookupOperation lookup = Aggregation.lookup("MCT_ARCHIVO", "identificadorArchivo", "_id", "archivoDTO");
		UnwindOperation unwind = Aggregation.unwind("archivoDTO");
		TypedAggregation<DetalleRegistroDTO> aggregation = Aggregation.newAggregation(DetalleRegistroDTO.class,
				Arrays.asList(match, lookup, unwind));
		AggregationResults<DetalleRegistroDTO> aggregationResults = mongoOperations.aggregate(aggregation,
				DetalleRegistroDTO.class);
		
		
		DetalleRegistroDTO detalle = aggregationResults.getUniqueMappedResult();
		if (detalle != null) {
			if(detalle.getArchivoDTO() != null) {
				detalle.setFecProcesoCarga(detalle.getArchivoDTO().getFecProcesoCarga());
				detalle.setFecProcesoCarga(detalle.getArchivoDTO().getFecProcesoCarga());
				detalle.getAseguradoDTO().setDesCodigoError(detalle.getAseguradoDTO().getCveCodigoError());
				detalle.setObjectIdOrigen(
						detalle.getObjectIdArchivoDetalle() != null ? detalle.getObjectIdArchivoDetalle().toString()
								: null);
				detalle.getPatronDTO()
						.setDesPrima(detalle.getPatronDTO().getNumPrima() != null
								? detalle.getPatronDTO().getNumPrima().toString()
								: "");
			}			
			
			MatchOperation matchCambios = Aggregation.match(Criteria.where("objectIdOrigen").is(new ObjectId(objectId)));
			TypedAggregation<CambioDTO> aggregationCambios = Aggregation.newAggregation(CambioDTO.class,
					Collections.singletonList(matchCambios));
			AggregationResults<CambioDTO> aggregationResultsCambios = mongoOperations.aggregate(aggregationCambios,
					CambioDTO.class);
			
			List<CambioDTO> listCambios = aggregationResultsCambios.getMappedResults();
			if(!listCambios.isEmpty()) {
				Optional<CambioDTO> cambio = listCambios.stream()
										.sorted(Comparator.comparing(CambioDTO::getFecAlta)
										.reversed())
										.findFirst();
				
				if(StringUtils.isEmpty(detalle.getDesSituacionRegistro())) {
					detalle.setDesSituacionRegistro(cambio.get().getDesSituacionRegistro());
				}				
				
				detalle.setAuditorias(new ArrayList<>());
				List<AuditoriaDTO> auditorias = new ArrayList<>();
				auditorias = cambio.get().getAuditorias().stream().map(au->{
					AuditoriaDTO resp = new AuditoriaDTO();
					
					resp.setAccion(au.getAccion());
					resp.setCveIdAccionRegistro(au.getCveIdAccionRegistro());
					resp.setCveSituacionRegistro(au.getCveSituacionRegistro());
					resp.setDesAccionRegistro(au.getDesAccionRegistro());
					resp.setDesCambio(au.getDesCambio());
					resp.setDesObservacionesAprobador(au.getDesObservacionesAprobador());
					resp.setDesObservacionesSol(au.getDesObservacionesSol());
					resp.setDesSituacionRegistro(au.getDesSituacionRegistro());
					resp.setFecActualizacion(au.getFecActualizacion());
					resp.setFecAlta(au.getFecAlta());
					resp.setFecBaja(au.getFecBaja());
					resp.setNomUsuario(au.getNomUsuario());
					resp.setNumFolioMovOriginal(au.getNumFolioMovOriginal());
					
					return resp;
				}).collect(Collectors.toList());
				detalle.getAuditorias().addAll(auditorias);
				
			}
			
		}

		return detalle;
	}
	
	@Override
	public DetalleRegistroDTO getDetalleMovimiento(@Valid String objectId) {
		// Se construye la agregacion
		MatchOperation match = Aggregation.match(Criteria.where("_id").is(new ObjectId(objectId)));
		LookupOperation lookup = Aggregation.lookup("MCT_ARCHIVO", "identificadorArchivo", "_id", "archivoDTO");
		UnwindOperation unwind = Aggregation.unwind("archivoDTO");
		TypedAggregation<DetalleRegistroDTO> aggregation = Aggregation.newAggregation(DetalleRegistroDTO.class,
				Arrays.asList(match, lookup, unwind));
		AggregationResults<DetalleRegistroDTO> aggregationResults = mongoOperations.aggregate(aggregation,
				DetalleRegistroDTO.class);

		DetalleRegistroDTO detalle = aggregationResults.getUniqueMappedResult();
		if (detalle != null) {
			if (detalle.getArchivoDTO() != null) {
				detalle.setFecProcesoCarga(detalle.getArchivoDTO().getFecProcesoCarga());
				detalle.setFecProcesoCarga(detalle.getArchivoDTO().getFecProcesoCarga());
				detalle.getAseguradoDTO().setDesCodigoError(detalle.getAseguradoDTO().getCveCodigoError());
				detalle.setObjectIdOrigen(
						detalle.getObjectIdArchivoDetalle() != null ? detalle.getObjectIdArchivoDetalle().toString()
								: null);
				detalle.getPatronDTO()
						.setDesPrima(detalle.getPatronDTO().getNumPrima() != null
								? detalle.getPatronDTO().getNumPrima().toString()
								: "");
			}

		}

		return detalle;
	}

	@Override
	public Object updateMovimiento(DatosModificadosDTO input) {
		DateFormat df = new SimpleDateFormat("yyyy");
		// Se construye el query
		Query query = new Query();
		ObjectId id = new ObjectId(input.getObjectIdOrigen());
		query.addCriteria(Criteria.where("_id").is(id));
		DetalleRegistroDTO detalle = mongoOperations.findOne(query, DetalleRegistroDTO.class);

		// se aniade la fecha de actualizacion al registro asi como el usuario que hizo
		// la actualizacion
		assert detalle != null;
		detalle.getAseguradoDTO().setFecActualizacion(DateUtils.getSysDateMongoISO());
		detalle.getAseguradoDTO().setUsuarioModificador(input.getUsuarioModificador());
		detalle.setIsPending(Boolean.FALSE);

		if (NumberUtils.isValid(input.getCveConsecuecniaModificado(), Boolean.FALSE)) {
			detalle.getIncapacidadDTO().setCveConsecuencia(input.getCveConsecuecniaModificado());
		}

		if (StringUtils.isValid(input.getDesConsecuenciaModificado())) {
			detalle.getIncapacidadDTO().setDesConsecuencia(input.getDesConsecuenciaModificado());
		}

		if (NumberUtils.isValid(input.getCveTipoRiesgoModificado(), Boolean.TRUE)) {
			detalle.getIncapacidadDTO().setCveTipoRiesgo(input.getCveTipoRiesgoModificado().toString());
		}

		if (StringUtils.isValid(input.getDesTipoRiesgoModificado())) {
			detalle.getIncapacidadDTO().setDesTipoRiesgo(input.getDesTipoRiesgoModificado());
		}

		if (input.getFecFinModificado() != null) {
			detalle.getIncapacidadDTO().setFecFin(input.getFecFinModificado());
		}

		if (input.getFecInicioModificado() != null) {
			detalle.getIncapacidadDTO().setFecInicio(input.getFecInicioModificado());
		}
		
		if(input.getBitacoraDictamen() != null) {
			List<BitacoraDictamenDTO> bitacora = input.
					getBitacoraDictamen().stream().filter(dic -> dic.isActivo())
				.collect(Collectors.toList());
			detalle.getIncapacidadDTO().setBitacoraDictamen(bitacora);
		}

		if (StringUtils.isValid(input.getNssModificado())) {
			detalle.getAseguradoDTO().setNumNss(input.getNssModificado());
		}

		if (NumberUtils.isValid(input.getNumDiasSubsidiadosModificado(), Boolean.FALSE)) {
			detalle.getIncapacidadDTO().setNumDiasSubsidiados(input.getNumDiasSubsidiadosModificado());
		}

		if (input.getPorcentajeIncapacidadModificado() != null) {
			detalle.getIncapacidadDTO().setPorPorcentajeIncapacidad(input.getPorcentajeIncapacidadModificado());
		}

		if (StringUtils.isValid(input.getRpModificado())) {
			// Se llenan los datos del patron
			CambioDTO cambioDTO = input.getCambioDTO();
			if (cambioDTO != null && detalle.getPatronDTO().getRefRegistroPatronal() != null &&
					!detalle.getPatronDTO().getRefRegistroPatronal().equals(input.getRpModificado())) {
				detalle.getPatronDTO().setCveClase(cambioDTO.getCveClase());
				detalle.getPatronDTO().setCveDelRegPatronal(cambioDTO.getCveDelRegPatronal());
				detalle.getPatronDTO().setCveFraccion(cambioDTO.getCveFraccion());
				detalle.getPatronDTO().setCveSubDelRegPatronal(cambioDTO.getCveSubDelRegPatronal());
				detalle.getPatronDTO().setDesClase(cambioDTO.getDesClase());
				detalle.getPatronDTO().setDesDelRegPatronal(cambioDTO.getDesDelRegPatronal());
				detalle.getPatronDTO().setDesFraccion(cambioDTO.getDesFraccion());
				detalle.getPatronDTO().setDesRazonSocial(cambioDTO.getDesRazonSocial());
				detalle.getPatronDTO().setDesRfc(cambioDTO.getDesRfc());
				detalle.getPatronDTO().setDesSubDelRegPatronal(cambioDTO.getDesSubDelRegPatronal());
				detalle.getPatronDTO().setNumPrima(cambioDTO.getNumPrima());
				detalle.getPatronDTO().setFecAlta(detalle.getPatronDTO().getFecAlta() == null ?
						DateUtils.getSysDateMongoISO() : detalle.getPatronDTO().getFecAlta());
			}
			detalle.getPatronDTO().setRefRegistroPatronal(input.getRpModificado());
		}


		if (input.getCveIdAccionRegistro() == AccionRegistroEnum.BAJA_PENDIENTE.getClave()
				|| input.getCveIdAccionRegistro() == AccionRegistroEnum.ELIMINACION.getClave()) {
			calculaRN68(input);
			detalle.getAseguradoDTO().setCveEstadoRegistro(input.getCveEstadoRegistro());
			detalle.getAseguradoDTO().setDesEstadoRegistro(input.getDesEstadoRegistro());
			detalle.getAseguradoDTO().setFecBaja(new Date());
		}
		if (input.getCveIdAccionRegistro() == AccionRegistroEnum.MODIFICACION_PENDIENTE.getClave()
				|| input.getCveIdAccionRegistro() == AccionRegistroEnum.MODIFICACION.getClave()) {
			if (input.getCveEstadoRegistro().equals(EstadoRegistroEnum.SUSCEPTIBLE.getCveEstadoRegistro())) {
				detalle.getAseguradoDTO().setCveEstadoRegistro(EstadoRegistroEnum.SUSCEPTIBLE.getCveEstadoRegistro());
				detalle.getAseguradoDTO().setDesEstadoRegistro(EstadoRegistroEnum.SUSCEPTIBLE.getDesDescripcion());
			} else if (input.getCveEstadoRegistro().equals(EstadoRegistroEnum.CORRECTO.getCveEstadoRegistro())) {
				detalle.getAseguradoDTO().setCveEstadoRegistro(EstadoRegistroEnum.CORRECTO.getCveEstadoRegistro());
				detalle.getAseguradoDTO().setDesEstadoRegistro(EstadoRegistroEnum.CORRECTO.getDesDescripcion());
			} else if (input.getCveEstadoRegistro().equals(EstadoRegistroEnum.ERRONEO.getCveEstadoRegistro())) {
				detalle.getAseguradoDTO().setCveEstadoRegistro(EstadoRegistroEnum.CORRECTO.getCveEstadoRegistro());
				detalle.getAseguradoDTO().setDesEstadoRegistro(EstadoRegistroEnum.CORRECTO.getDesDescripcion());
				correctData(detalle);
			} else if (input.getCveEstadoRegistro().equals(EstadoRegistroEnum.ERRONEO_OTRAS.getCveEstadoRegistro())) {
				detalle.getAseguradoDTO()
						.setCveEstadoRegistro(EstadoRegistroEnum.CORRECTO_OTRAS.getCveEstadoRegistro());
				detalle.getAseguradoDTO().setDesEstadoRegistro(EstadoRegistroEnum.CORRECTO_OTRAS.getDesDescripcion());
				correctData(detalle);
			}
		}
		if (input.getCveIdAccionRegistro() == AccionRegistroEnum.CONFIRMAR_PENDIENTE.getClave()
				|| input.getCveIdAccionRegistro() == AccionRegistroEnum.CONFIRMAR.getClave()) {
			if (Arrays.asList(4).contains(detalle.getAseguradoDTO().getCveEstadoRegistro())) {
				detalle.getAseguradoDTO().setCveEstadoRegistro(EstadoRegistroEnum.CORRECTO.getCveEstadoRegistro());
				detalle.getAseguradoDTO().setDesEstadoRegistro(EstadoRegistroEnum.CORRECTO.getDesDescripcion());
			}
			else if (Arrays.asList(8).contains(detalle.getAseguradoDTO().getCveEstadoRegistro())) {
				detalle.getAseguradoDTO().setCveEstadoRegistro(EstadoRegistroEnum.CORRECTO_OTRAS.getCveEstadoRegistro());
				detalle.getAseguradoDTO().setDesEstadoRegistro(EstadoRegistroEnum.CORRECTO_OTRAS.getDesDescripcion());
			}
			detalle.setConfirmarSinCambios(true);
		}
		detalle.getAseguradoDTO()
				.setNumCicloAnual(input.getFecFinModificado() != null ? df.format(input.getFecFinModificado()) : "");
		
		if(input.getCveIdAccionRegistro() != AccionRegistroEnum.BAJA_PENDIENTE.getClave()
				&& input.getCveIdAccionRegistro() != AccionRegistroEnum.ELIMINACION.getClave()) {
			CasoRegistroDTO caso = DateUtils.obtenerCasoRegistro(input.getFecFinModificado());
			detalle.getAseguradoDTO().setCveCasoRegistro(caso.getIdCaso() > 0 ? caso.getIdCaso() : null);
			detalle.getAseguradoDTO().setDesCasoRegistro(caso.getDescripcion());	
		}
		mongoOperations.save(detalle);

		return true;
	}

	private void correctData(DetalleRegistroDTO detalle) {
		detalle.getAseguradoDTO().setCveCodigoError(null);
		detalle.getAseguradoDTO().setDesCodigoError(null);
		List<AuditoriaDTO> auditList = new ArrayList<>();
		AuditoriaDTO auditoria = new AuditoriaDTO();
		auditoria.setBitacoraErroresDTO(detalle.getBitacoraErroresDTO());
		auditList.add(auditoria);
		detalle.setAuditorias(auditList);
		detalle.setBitacoraErroresDTO(null);
		detalle.getAseguradoDTO().setFecActualizacion(DateUtils.getSysDateMongoISO());
	}

	public DetalleRegistroDTO findOne(String objectId) {
		Query query = new Query();
		ObjectId id = new ObjectId(objectId);
		query.addCriteria(Criteria.where("_id").is(id));
		return mongoOperations.findOne(query, DetalleRegistroDTO.class);
	}

	@Override
	public Object updateMovimientoSusceptible(DetalleRegistroDTO input) {
		Query query = new Query();
		query.addCriteria(Criteria.where("_id").is(input.getObjectIdArchivoDetalle()));
		Update update = new Update();
		update.set("aseguradoDTO.cveEstadoRegistro", input.getAseguradoDTO().getCveEstadoRegistro());
		update.set("aseguradoDTO.desEstadoRegistro", input.getAseguradoDTO().getDesEstadoRegistro());
		update.set("aseguradoDTO.fecActualizacion", DateUtils.getSysDateMongoISO());
		update.set("auditorias", input.getAuditorias());
		return mongoOperations.updateFirst(query, update, DetalleRegistroDTO.class);

	}

	@Override
	public Object updateMovimientoSinCambios(DatosModificadosDTO input) {
		Query query = new Query();
		ObjectId id = new ObjectId(input.getObjectIdOrigen());
		query.addCriteria(Criteria.where("_id").is(id));
		DetalleRegistroDTO detalle = mongoOperations.findOne(query, DetalleRegistroDTO.class);

		assert detalle != null;
		List<AuditoriaDTO> auditorias = detalle.getAuditorias();
		if (auditorias != null) {
			for (AuditoriaDTO auditoriaDTO : auditorias) {
				auditoriaDTO.setCveIdAccionRegistro(input.getCveIdAccionRegistro());
				auditoriaDTO.setDesAccionRegistro(
						input.getCveIdAccionRegistro() == 11 ? AccionRegistroEnum.CONFIRMAR_PENDIENTE.getDescripcion()
								: AccionRegistroEnum.CONFIRMAR.getDescripcion());
				auditoriaDTO.setFecAlta(new Date());
				auditoriaDTO.setCveSituacionRegistro(SituacionRegistroEnum.PENDIENTE.getClave());
				auditoriaDTO.setDesSituacionRegistro(SituacionRegistroEnum.PENDIENTE.getDescripcion());				
			}
		}
		mongoOperations.save(detalle);
		return true;
	}
	
	private DatosModificadosDTO calculaRN68(DatosModificadosDTO input) {
		if (input.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.CORRECTO.getCveEstadoRegistro()
				|| input.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.DUPLICADO.getCveEstadoRegistro()
				|| input.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.ERRONEO.getCveEstadoRegistro()
				|| input.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.SUSCEPTIBLE.getCveEstadoRegistro()
				|| input.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.BAJA.getCveEstadoRegistro()) {
			input.setCveEstadoRegistro(EstadoRegistroEnum.BAJA.getCveEstadoRegistro());
			input.setDesEstadoRegistro(EstadoRegistroEnum.BAJA.getDesDescripcion());

		}
		if (input.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.CORRECTO_OTRAS.getCveEstadoRegistro()
				|| input.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.DUPLICADO_OTRAS
						.getCveEstadoRegistro()
				|| input.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.ERRONEO_OTRAS.getCveEstadoRegistro()
				|| input.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.SUSCEPTIBLE_OTRAS
						.getCveEstadoRegistro()
				|| input.getCveEstadoRegistro().intValue() == EstadoRegistroEnum.BAJA_OTRAS_DELEGACIONES
						.getCveEstadoRegistro()) {
			input.setCveEstadoRegistro(EstadoRegistroEnum.BAJA_OTRAS_DELEGACIONES.getCveEstadoRegistro());
			input.setDesEstadoRegistro(EstadoRegistroEnum.BAJA_OTRAS_DELEGACIONES.getDesDescripcion());

		}
		return input;
	}

	public void actualizaCifras(ArchivoDTO archivoDTO) {
		mongoOperations.updateFirst(Query.query(Criteria.where("_id").is(archivoDTO.getObjectIdArchivo())),
				Update.update("cifrasControlDTO", archivoDTO.getCifrasControlDTO()), ArchivoDTO.class);
	}

	@Override
	public Optional<ArchivoDTO> findOneById(String archivoId) {

		ArchivoDTO d = this.mongoOperations.findOne(new Query(Criteria.where("objectIdArchivo").is(archivoId)),
				ArchivoDTO.class);

		Optional<ArchivoDTO> user = Optional.ofNullable(d);

		return user;

	}

	@Override
	public void markAsPending(String objectId, Boolean isPending) {
		Query query = new Query(Criteria.where("objectIdArchivoDetalle").is(new ObjectId(objectId)));
		DetalleRegistroDTO detalleRegistroDTO = mongoOperations.findOne(query, DetalleRegistroDTO.class);
		assert detalleRegistroDTO != null;
		detalleRegistroDTO.setIsPending(isPending);
		mongoOperations.save(detalleRegistroDTO);
	}

}
