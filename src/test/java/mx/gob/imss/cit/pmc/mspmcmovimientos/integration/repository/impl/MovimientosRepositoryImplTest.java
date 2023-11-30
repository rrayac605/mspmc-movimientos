package mx.gob.imss.cit.pmc.mspmcmovimientos.integration.repository.impl;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import mx.gob.imss.cit.mspmccommons.integration.model.MovimientosOutputDTO;
import mx.gob.imss.cit.mspmccommons.integration.model.ResponseDTO;
import mx.gob.imss.cit.pmc.mspmcmovimientos.exception.BusinessException;
import mx.gob.imss.cit.pmc.mspmcmovimientos.integration.repository.MovimientosRepository;

@SpringBootTest
public class MovimientosRepositoryImplTest {

	@Autowired
	MovimientosRepository movimientosRepository;

	@Test
	void consultarPorCriterios() {

		Integer cveDelegacion = null;
		Integer cveSubdelegacion = null;
		Integer cveTipoRiesgo = null;

		Integer cveConsecuencia = null;

		Integer cveCasoRegistro = null;

		String fromMonth = "01";

		String fromYear = "2020";

		String toMonth = "06";

		String toYear = "2020";

		Integer cveEstadoRegistro = 1;

		List<Integer> cveEstadoRegistroList = null;

		String numNss = null;

		String refRegistroPatronal = null;

		String cveSituacionRegistro = null;

		try {
			ResponseDTO<List<MovimientosOutputDTO>> findMovimientos = movimientosRepository.findMovimientos(cveDelegacion,
					cveSubdelegacion, cveTipoRiesgo, cveConsecuencia, cveCasoRegistro, fromMonth, fromYear, toMonth,
					toYear, cveEstadoRegistro, cveEstadoRegistroList, numNss, refRegistroPatronal,
					cveSituacionRegistro, 1l, null, null);

//			assertNotNull(findMovimientos);
//			assertEquals(findMovimientos.size(), 28);
		} catch (BusinessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	void consultarDetalleMovimiento() {
		try {
			Object object = movimientosRepository.getDetalleMovimiento("5e7a6819a96e6321d93b1d4a", "43129374311", 23,
					"30819113");
		} catch (Exception e) {
			// TODO: handle exception
		}
	}

}